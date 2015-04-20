/*
 ***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2003 Joey Gibson <joey@joeygibson.com>
 * Copyright (C) 2004-2007 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004-2007 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2006 Miguel Covarrubias <mlcovarrubias@gmail.com>
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

import jnr.constants.platform.OpenFlags;
import jnr.posix.POSIX;
import org.jcodings.Encoding;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFileAttributeView;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import jnr.posix.FileStat;
import jnr.posix.util.Platform;
import org.jruby.runtime.Block;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import static org.jruby.runtime.Visibility.*;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.encoding.EncodingCapable;
import org.jruby.util.ByteList;
import org.jruby.util.FileResource;
import org.jruby.util.JRubyFile;
import org.jruby.util.StringSupport;
import org.jruby.util.TypeConverter;
import org.jruby.util.io.EncodingUtils;
import org.jruby.util.io.IOEncodable;
import org.jruby.util.io.ModeFlags;
import org.jruby.util.io.OpenFile;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.encoding.EncodingService;
import org.jruby.util.io.PosixShim;

import static org.jruby.util.io.EncodingUtils.vmode;
import static org.jruby.util.io.EncodingUtils.vperm;

/**
 * Ruby File class equivalent in java.
 **/
@JRubyClass(name="File", parent="IO", include="FileTest")
public class RubyFile extends RubyIO implements EncodingCapable {
    public static RubyClass createFileClass(Ruby runtime) {
        ThreadContext context = runtime.getCurrentContext();

        RubyClass fileClass = runtime.defineClass("File", runtime.getIO(), FILE_ALLOCATOR);

        runtime.setFile(fileClass);

        fileClass.defineAnnotatedMethods(RubyFile.class);

        fileClass.setClassIndex(ClassIndex.FILE);
        fileClass.setReifiedClass(RubyFile.class);

        fileClass.kindOf = new RubyModule.JavaClassKindOf(RubyFile.class);

        // file separator constants
        RubyString separator = runtime.newString("/");
        separator.freeze(context);
        fileClass.defineConstant("SEPARATOR", separator);
        fileClass.defineConstant("Separator", separator);

        if (File.separatorChar == '\\') {
            RubyString altSeparator = runtime.newString("\\");
            altSeparator.freeze(context);
            fileClass.defineConstant("ALT_SEPARATOR", altSeparator);
        } else {
            fileClass.defineConstant("ALT_SEPARATOR", runtime.getNil());
        }

        // path separator
        RubyString pathSeparator = runtime.newString(File.pathSeparator);
        pathSeparator.freeze(context);
        fileClass.defineConstant("PATH_SEPARATOR", pathSeparator);

        // For JRUBY-5276, physically define FileTest methods on File's singleton
        fileClass.getSingletonClass().defineAnnotatedMethods(RubyFileTest.FileTestFileMethods.class);

        // Create Constants class
        RubyModule constants = fileClass.defineModuleUnder("Constants");

        // open flags
        /* open for reading only */
        constants.setConstant("RDONLY", runtime.newFixnum(OpenFlags.O_RDONLY.intValue()));
        /* open for writing only */
        constants.setConstant("WRONLY", runtime.newFixnum(OpenFlags.O_WRONLY.intValue()));
        /* open for reading and writing */
        constants.setConstant("RDWR", runtime.newFixnum(OpenFlags.O_RDWR.intValue()));
        /* append on each write */
        constants.setConstant("APPEND", runtime.newFixnum(OpenFlags.O_APPEND.intValue()));
        /* create file if it does not exist */
        constants.setConstant("CREAT", runtime.newFixnum(OpenFlags.O_CREAT.intValue()));
        /* error if CREAT and the file exists */
        constants.setConstant("EXCL", runtime.newFixnum(OpenFlags.O_EXCL.intValue()));
        if (    // O_NDELAY not defined in OpenFlags
                //OpenFlags.O_NDELAY.defined() ||
                OpenFlags.O_NONBLOCK.defined()) {
            if (!OpenFlags.O_NONBLOCK.defined()) {
//                #   define O_NONBLOCK O_NDELAY
            }
            /* do not block on open or for data to become available */
            constants.setConstant("NONBLOCK", runtime.newFixnum(OpenFlags.O_NONBLOCK.intValue()));
        } else if (Platform.IS_WINDOWS) {
            // FIXME: Should NONBLOCK exist for Windows fcntl flags?
            constants.setConstant("NONBLOCK", runtime.newFixnum(1));
        }
        /* truncate size to 0 */
        constants.setConstant("TRUNC", runtime.newFixnum(OpenFlags.O_TRUNC.intValue()));
        // FIXME: NOCTTY is showing up as undefined on Linux, but it should be defined.
//        if (OpenFlags.O_NOCTTY.defined()) {
            /* not to make opened IO the controlling terminal device */
            constants.setConstant("NOCTTY", runtime.newFixnum(OpenFlags.O_NOCTTY.intValue()));
//        }
        if (!OpenFlags.O_BINARY.defined()) {
            constants.setConstant("BINARY", runtime.newFixnum(0));
        } else {
            /* disable line code conversion */
            constants.setConstant("BINARY", runtime.newFixnum(OpenFlags.O_BINARY.intValue()));
        }
        if (OpenFlags.O_SYNC.defined()) {
            /* any write operation perform synchronously */
            constants.setConstant("SYNC", runtime.newFixnum(OpenFlags.O_SYNC.intValue()));
        }
        // O_DSYNC and O_RSYNC are not in OpenFlags
//        #ifdef O_DSYNC
//        /* any write operation perform synchronously except some meta data */
//        constants.setConstant("DSYNC", runtime.newFixnum(OpenFlags.O_DSYNC.intValue()));
//        #endif
//        #ifdef O_RSYNC
//        /* any read operation perform synchronously. used with SYNC or DSYNC. */
//        constants.setConstant("RSYNC", runtime.newFixnum(OpenFlags.O_RSYNC.intValue()));
//        #endif
        if (OpenFlags.O_NOFOLLOW.defined()) {
            /* do not follow symlinks */
            constants.setConstant("NOFOLLOW", runtime.newFixnum(OpenFlags.O_NOFOLLOW.intValue()));     /* FreeBSD, Linux */
        }
        // O_NOATIME and O_DIRECT are not in OpenFlags
//        #ifdef O_NOATIME
//        /* do not change atime */
//        constants.setConstant("NOATIME", runtime.newFixnum(OpenFlags.O_NOATIME.intValue()));     /* Linux */
//        #endif
//        #ifdef O_DIRECT
//        /*  Try to minimize cache effects of the I/O to and from this file. */
//        constants.setConstant("DIRECT", runtime.newFixnum(OpenFlags.O_DIRECT.intValue()));
//        #endif

        // case handling, escaping, path and dot matching
        constants.setConstant("FNM_NOESCAPE", runtime.newFixnum(FNM_NOESCAPE));
        constants.setConstant("FNM_CASEFOLD", runtime.newFixnum(FNM_CASEFOLD));
        constants.setConstant("FNM_SYSCASE", runtime.newFixnum(FNM_SYSCASE));
        constants.setConstant("FNM_DOTMATCH", runtime.newFixnum(FNM_DOTMATCH));
        constants.setConstant("FNM_PATHNAME", runtime.newFixnum(FNM_PATHNAME));
        constants.setConstant("FNM_EXTGLOB", runtime.newFixnum(FNM_EXTGLOB));

        // flock operations
        constants.setConstant("LOCK_SH", runtime.newFixnum(RubyFile.LOCK_SH));
        constants.setConstant("LOCK_EX", runtime.newFixnum(RubyFile.LOCK_EX));
        constants.setConstant("LOCK_NB", runtime.newFixnum(RubyFile.LOCK_NB));
        constants.setConstant("LOCK_UN", runtime.newFixnum(RubyFile.LOCK_UN));
        
        // NULL device
        constants.setConstant("NULL", runtime.newString(getNullDevice()));

        // File::Constants module is included in IO.
        runtime.getIO().includeModule(constants);

        return fileClass;
    }

    private static ObjectAllocator FILE_ALLOCATOR = new ObjectAllocator() {
        @Override
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            RubyFile instance = new RubyFile(runtime, klass);

            instance.setMetaClass(klass);

            return instance;
        }
    };

    private static String getNullDevice() {
        // FIXME: MRI defines special null device for Amiga and VMS, but currently
        // we lack ability to detect these platforms
        String null_device;
        if (Platform.IS_WINDOWS) {
            null_device = "NUL";
        } else {
            null_device = "/dev/null";
        }
        return null_device;
    }

    public RubyFile(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }

    // XXX This constructor is a hack to implement the __END__ syntax.
    //     Converting a reader back into an InputStream doesn't generally work.
    public RubyFile(Ruby runtime, String path, final Reader reader) {
        this(runtime, path, new InputStream() {
            @Override
            public int read() throws IOException {
                return reader.read();
            }
        });
    }

    public RubyFile(Ruby runtime, String path, InputStream in) {
        super(runtime, runtime.getFile(), Channels.newChannel(in));
        this.setPath(path);
    }
    
    @Override
    protected IRubyObject rbIoClose(Ruby runtime) {
        // Make sure any existing lock is released before we try and close the file
        if (openFile.currentLock != null) {
            try {
                openFile.currentLock.release();
            } catch (IOException e) {
                throw getRuntime().newIOError(e.getMessage());
            }
        }
        return super.rbIoClose(runtime);
    }

    @JRubyMethod(required = 1)
    public IRubyObject flock(ThreadContext context, IRubyObject operation) {
        Ruby runtime = context.runtime;
        OpenFile fptr;
//        int[] op = {0,0};
        int op1;
//        struct timeval time;

//        rb_secure(2);
        op1 = RubyNumeric.num2int(operation);
        fptr = getOpenFileChecked();

        if (fptr.isWritable()) {
            flushRaw(context, false);
        }

        // MRI's logic differs here. For a nonblocking flock that produces EAGAIN, EACCES, or EWOULBLOCK, MRI
        // just returns false immediately. For the same errnos in blocking mode, MRI waits for 0.1s and then
        // attempts the lock again, indefinitely.
        while (fptr.threadFlock(context, op1) < 0) {
            switch (fptr.errno()) {
                case EAGAIN:
                case EACCES:
                case EWOULDBLOCK:
                    if ((op1 & LOCK_NB) != 0) return runtime.getFalse();

                    try {
                        context.getThread().sleep(100);
                    } catch (InterruptedException ie) {
                        context.pollThreadEvents();
                    }
                    fptr.checkClosed();
                    continue;

                case EINTR:
                    break;

                default:
                    throw runtime.newErrnoFromErrno(fptr.errno(), fptr.getPath());
            }
        }
        return RubyFixnum.zero(context.runtime);
    }

    // rb_file_initialize
    @JRubyMethod(name = "initialize", required = 1, optional = 2, visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject[] args, Block block) {
        if (openFile != null) {
            throw context.runtime.newRuntimeError("reinitializing File");
        }

        if (args.length > 0 && args.length <= 3) {
            IRubyObject fd = TypeConverter.convertToTypeWithCheck(args[0], context.runtime.getFixnum(), "to_int");
            if (!fd.isNil()) {
                if (args.length == 1) {
                    return super.initialize(context, fd, block);
                } else if (args.length == 2) {
                    return super.initialize(context, fd, args[1], block);
                }
                return super.initialize(context, fd, args[1], args[2], block);
            }
        }

        return openFile(context, args);
    }

    @JRubyMethod(required = 1)
    public IRubyObject chmod(ThreadContext context, IRubyObject arg) {
        checkClosed(context);
        int mode = (int) arg.convertToInteger().getLongValue();

        if (!new File(getPath()).exists()) {
            throw context.runtime.newErrnoENOENTError(getPath());
        }

        return context.runtime.newFixnum(context.runtime.getPosix().chmod(getPath(), mode));
    }

    @JRubyMethod(required = 2)
    public IRubyObject chown(ThreadContext context, IRubyObject arg1, IRubyObject arg2) {
        checkClosed(context);
        int owner = -1;
        if (!arg1.isNil()) {
            owner = RubyNumeric.num2int(arg1);
        }

        int group = -1;
        if (!arg2.isNil()) {
            group = RubyNumeric.num2int(arg2);
        }

        if (!new File(getPath()).exists()) {
            throw context.runtime.newErrnoENOENTError(getPath());
        }

        return context.runtime.newFixnum(context.runtime.getPosix().chown(getPath(), owner, group));
    }

    @JRubyMethod
    public IRubyObject atime(ThreadContext context) {
        checkClosed(context);
        return context.runtime.newFileStat(getPath(), false).atime();
    }

    @JRubyMethod(name = "ctime")
    public IRubyObject ctime(ThreadContext context) {
        checkClosed(context);
        return context.runtime.newFileStat(getPath(), false).ctime();
    }

    @JRubyMethod(name = "birthtime")
    public IRubyObject birthtime(ThreadContext context) {
        checkClosed(context);

        FileTime btime = getBirthtimeWithNIO(getPath());
        if (btime != null) return context.runtime.newTime(btime.toMillis()); // btime comes in nanos
        return ctime(context);
    }

    public static final FileTime getBirthtimeWithNIO(String pathString) {
        // FIXME: birthtime is in stat, so we should use that if platform supports it (#2152)
        // TODO: This requires Java 7 APIs and may not work on Android
        Path path = Paths.get(pathString);
        PosixFileAttributeView view = Files.getFileAttributeView(path, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
        try {
            if (view != null) {
                return view.readAttributes().creationTime();
            }
        } catch (IOException ioe) {
            // ignore, just fall back on ctime
        }
        return null;
    }

    @JRubyMethod
    public IRubyObject lstat(ThreadContext context) {
        checkClosed(context);
        return context.runtime.newFileStat(getPath(), true);
    }
    
    @JRubyMethod
    public IRubyObject mtime(ThreadContext context) {
        checkClosed(context);
        return context.runtime.newFileStat(getPath(), false).mtime();
    }

    @JRubyMethod(meta = true)
    public static IRubyObject path(ThreadContext context, IRubyObject self, IRubyObject str) {
        return StringSupport.checkEmbeddedNulls(context.runtime, get_path(context, str));
    }

    @JRubyMethod(name = {"path", "to_path"})
    public IRubyObject path(ThreadContext context) {
        IRubyObject newPath = context.runtime.getNil();
        if (getPath() != null) {
            newPath = context.runtime.newString(getPath());
            newPath.setTaint(true);
        }
        return newPath;
    }

    @JRubyMethod(required = 1)
    public IRubyObject truncate(ThreadContext context, IRubyObject len) {
        Ruby runtime = context.runtime;
        OpenFile fptr;
        long pos;

        pos = RubyNumeric.num2int(len);
        fptr = getOpenFileChecked();
        if (!fptr.isWritable()) {
            throw runtime.newIOError("not opened for writing");
        }
        flushRaw(context, false);

        if (pos < 0) {
            throw runtime.newErrnoEINVALError(openFile.getPath());
        }

        if (fptr.posix.ftruncate(fptr.fd(), pos) < 0) {
            throw runtime.newErrnoFromErrno(fptr.posix.errno, fptr.getPath());
        }

        return RubyFixnum.zero(runtime);
    }

    @JRubyMethod
    @Override
    public IRubyObject inspect() {
        StringBuilder val = new StringBuilder();
        val.append("#<File:").append(getPath());
        if(!openFile.isOpen()) {
            val.append(" (closed)");
        }
        val.append(">");
        return getRuntime().newString(val.toString());
    }
    
    /* File class methods */
    
    @JRubyMethod(required = 1, optional = 1, meta = true)
    public static IRubyObject basename(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = context.runtime;

        RubyString origString = StringSupport.checkEmbeddedNulls(runtime, get_path(context, args[0]));
        Encoding origEncoding = origString.getEncoding();
        String name = origString.toString();

        // MRI-compatible basename handling for windows drive letter paths
        if (Platform.IS_WINDOWS) {
            if (name.length() > 1 && name.charAt(1) == ':' && Character.isLetter(name.charAt(0))) {
                switch (name.length()) {
                case 2:
                    return RubyString.newEmptyString(runtime, origString.getEncoding()).infectBy(args[0]);
                case 3:
                    if (origEncoding.getCharset() != null) {
                        try {
                            return RubyString.newString(runtime, new ByteList(name.substring(2).getBytes(origEncoding.getCharsetName()), origString.getEncoding())).infectBy(args[0]);
                        } catch (UnsupportedEncodingException uee) {
                            // fall through to UTF-8 logic
                        }
                    }

                    return RubyString.newString(runtime, name.substring(2)).infectBy(args[0]);
                default:
                    switch (name.charAt(2)) {
                    case '/':
                    case '\\':
                        break;
                    default:
                        // strip c: away from relative-pathed name
                        name = name.substring(2);
                        break;
                    }
                    break;
                }
            }
        }

        while (name.length() > 1 && name.charAt(name.length() - 1) == '/') {
            name = name.substring(0, name.length() - 1);
        }
        
        // Paths which end in "/" or "\\" must be stripped off.
        int slashCount = 0;
        int length = name.length();
        for (int i = length - 1; i >= 0; i--) {
            char c = name.charAt(i);
            if (c != '/' && c != '\\') {
                break;
            }
            slashCount++;
        }
        if (slashCount > 0 && length > 1) {
            name = name.substring(0, name.length() - slashCount);
        }
        
        int index = name.lastIndexOf('/');
        if (index == -1) {
            // XXX actually only on windows...
            index = name.lastIndexOf('\\');
        }
        
        if (!name.equals("/") && index != -1) {
            name = name.substring(index + 1);
        }
        
        if (args.length == 2) {
            String ext = RubyString.stringValue(args[1]).toString();
            if (".*".equals(ext)) {
                index = name.lastIndexOf('.');
                if (index > 0) {  // -1 no match; 0 it is dot file not extension
                    name = name.substring(0, index);
                }
            } else if (name.endsWith(ext)) {
                name = name.substring(0, name.length() - ext.length());
            }
        }
        if (origEncoding.getCharset() != null) {
            try {
                return RubyString.newString(runtime, new ByteList(name.getBytes(origEncoding.getCharsetName()), origString.getEncoding())).infectBy(args[0]);
            } catch (UnsupportedEncodingException uee) {
                // fall through to UTF-8 logic
            }
        }

        return RubyString.newString(runtime, name).infectBy(args[0]);
    }

    @JRubyMethod(required = 2, rest = true, meta = true)
    public static IRubyObject chmod(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        
        int count = 0;
        RubyInteger mode = args[0].convertToInteger();
        for (int i = 1; i < args.length; i++) {
            JRubyFile filename = file(args[i]);
            
            if (!filename.exists()) {
                throw runtime.newErrnoENOENTError(filename.toString());
            }
            
            if (0 != runtime.getPosix().chmod(filename.getAbsolutePath(), (int)mode.getLongValue())) {
                throw runtime.newErrnoFromLastPOSIXErrno();
            } else {
                count++;
            }
        }
        
        return runtime.newFixnum(count);
    }
    
    @JRubyMethod(required = 2, rest = true, meta = true)
    public static IRubyObject chown(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = context.runtime;

        int count = 0;
        int owner = -1;
        if (!args[0].isNil()) {
            owner = RubyNumeric.num2int(args[0]);
        }

        int group = -1;
        if (!args[1].isNil()) {
            group = RubyNumeric.num2int(args[1]);
        }
        for (int i = 2; i < args.length; i++) {
            JRubyFile filename = file(args[i]);

            if (!filename.exists()) {
                throw runtime.newErrnoENOENTError(filename.toString());
            }
            
            if (0 != runtime.getPosix().chown(filename.getAbsolutePath(), owner, group)) {
                throw runtime.newErrnoFromLastPOSIXErrno();
            } else {
                count++;
            }
        }
        
        return runtime.newFixnum(count);
    }

    @JRubyMethod(required = 1, meta = true)
    public static IRubyObject dirname(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        Ruby runtime = context.runtime;
        RubyString filename = StringSupport.checkEmbeddedNulls(runtime, get_path(context, arg));

        String jfilename = filename.asJavaString();

        return runtime.newString(dirname(context, jfilename)).infectBy(filename);
    }

    public static String dirname(ThreadContext context, String jfilename) {
        String name = jfilename.replace('\\', '/');
        int minPathLength = 1;
        boolean trimmedSlashes = false;

        boolean startsWithDriveLetterOnWindows = startsWithDriveLetterOnWindows(name);

        if (startsWithDriveLetterOnWindows) {
            minPathLength = 3;
        }

        while (name.length() > minPathLength && name.charAt(name.length() - 1) == '/') {
            trimmedSlashes = true;
            name = name.substring(0, name.length() - 1);
        }

        String result;
        if (startsWithDriveLetterOnWindows && name.length() == 2) {
            if (trimmedSlashes) {
                // C:\ is returned unchanged
                result = jfilename.substring(0, 3);
            } else {
                result = jfilename.substring(0, 2) + '.';
            }
        } else {
            //TODO deal with UNC names
            int index = name.lastIndexOf('/');

            if (index == -1) {
                if (startsWithDriveLetterOnWindows) {
                    return jfilename.substring(0, 2) + ".";
                } else {
                    return ".";
                }
            }
            if (index == 0) {
                return "/";
            }

            if (startsWithDriveLetterOnWindows && index == 2) {
                // Include additional path separator
                // (so that dirname of "C:\file.txt" is  "C:\", not "C:")
                index++;
            }

            if (jfilename.startsWith("\\\\")) {
                index = jfilename.length();
                String[] splitted = jfilename.split(Pattern.quote("\\"));
                int last = splitted.length-1;
                if (splitted[last].contains(".")) {
                    index = jfilename.lastIndexOf("\\");
                }
                
            }
            
            result = jfilename.substring(0, index);
            
        }
        
        char endChar;
        // trim trailing slashes
        while (result.length() > minPathLength) {
            endChar = result.charAt(result.length() - 1);
            if (endChar == '/' || endChar == '\\') {
                result = result.substring(0, result.length() - 1);
            } else {
                break;
            }
        }

        return result;
    }

    /**
     * Returns the extension name of the file. An empty string is returned if 
     * the filename (not the entire path) starts or ends with a dot.
     * @param recv
     * @param arg Path to get extension name of
     * @return Extension, including the dot, or an empty string
     */
    @JRubyMethod(required = 1, meta = true)
    public static IRubyObject extname(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        IRubyObject baseFilename = basename(context, recv, new IRubyObject[]{arg});
        
        String filename = RubyString.stringValue(baseFilename).getUnicodeValue();
        String result = "";

        int dotIndex = filename.lastIndexOf(".");
        if (dotIndex > 0 && dotIndex != (filename.length() - 1)) {
            // Dot is not at beginning and not at end of filename. 
            result = filename.substring(dotIndex);
        }

        return context.runtime.newString(result);
    }

    /**
     * Converts a pathname to an absolute pathname. Relative paths are 
     * referenced from the current working directory of the process unless 
     * a second argument is given, in which case it will be used as the 
     * starting point. If the second argument is also relative, it will 
     * first be converted to an absolute pathname.
     * @param recv
     * @param args 
     * @return Resulting absolute path as a String
     */
    public static IRubyObject expand_path(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return expand_path19(context, recv, args);
    }

    @JRubyMethod(name = "expand_path", required = 1, optional = 1, meta = true)
    public static IRubyObject expand_path19(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        RubyString path = (RubyString) expandPathInternal(context, recv, args, true, false);
        path.force_encoding(context, context.runtime.getEncodingService().getDefaultExternal());

        return path;
    }


    /**
     * ---------------------------------------------------- File::absolute_path
     *      File.absolute_path(file_name [, dir_string] ) -> abs_file_name
     *
     *      From Ruby 1.9.1
     * ------------------------------------------------------------------------
     *      Converts a pathname to an absolute pathname. Relative paths are
     *      referenced from the current working directory of the process unless
     *      _dir_string_ is given, in which case it will be used as the
     *      starting point. If the given pathname starts with a ``+~+'' it is
     *      NOT expanded, it is treated as a normal directory name.
     *
     *         File.absolute_path("~oracle/bin")       #=> "<relative_path>/~oracle/bin"
     *
     * @param context
     * @param recv
     * @param args
     * @return
     */
    @JRubyMethod(required = 1, optional = 1, meta = true)
    public static IRubyObject absolute_path(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return expandPathInternal(context, recv, args, false, false);
    }

    @JRubyMethod(required = 1, optional = 1, meta = true)
    public static IRubyObject realdirpath(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return expandPathInternal(context, recv, args, false, false);
    }

    @JRubyMethod(required = 1, optional = 1, meta = true)
    public static IRubyObject realpath(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        IRubyObject file = expandPathInternal(context, recv, args, false, true);
        if (!RubyFileTest.exist_p(recv, file).isTrue()) {
            throw context.runtime.newErrnoENOENTError(file.toString());
        }
        return file;
    }

    /**
     * Returns true if path matches against pattern The pattern is not a regular expression;
     * instead it follows rules similar to shell filename globbing. It may contain the following
     * metacharacters:
     *   *:  Glob - match any sequence chars (re: .*).  If like begins with '.' then it doesn't.
     *   ?:  Matches a single char (re: .).
     *   [set]:  Matches a single char in a set (re: [...]).
     *
     */
    @JRubyMethod(name = {"fnmatch", "fnmatch?"}, required = 2, optional = 1, meta = true)
    public static IRubyObject fnmatch(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        int flags = args.length == 3 ? RubyNumeric.num2int(args[2]) : 0;
        boolean braces_match = false;
        boolean extglob = (flags & FNM_EXTGLOB) != 0;

        ByteList pattern = args[0].convertToString().getByteList();
        ByteList path = StringSupport.checkEmbeddedNulls(runtime, get_path(context, args[1])).getByteList();

        if(extglob) {
            String spattern = args[0].asJavaString();
            ArrayList<String> patterns = org.jruby.util.Dir.braces(spattern, flags, new ArrayList<String>());

            ArrayList<Boolean> matches = new ArrayList<Boolean>();
            for(int i = 0; i < patterns.size(); i++) {
                String p = patterns.get(i);
                boolean match = dir_fnmatch(new ByteList(p.getBytes()), path, flags);
                matches.add(match);
            }
            braces_match = matches.contains(true);
        }

        if(braces_match || dir_fnmatch(pattern, path, flags)) {
            return runtime.getTrue();
        }
        return runtime.getFalse();
    }

    private static boolean dir_fnmatch(ByteList pattern, ByteList path, int flags) {
        return org.jruby.util.Dir.fnmatch(pattern.getUnsafeBytes(),
            pattern.getBegin(),
            pattern.getBegin()+pattern.getRealSize(),
            path.getUnsafeBytes(),
            path.getBegin(),
            path.getBegin()+path.getRealSize(),
            flags) == 0;
    }

    @JRubyMethod(name = "ftype", required = 1, meta = true)
    public static IRubyObject ftype(ThreadContext context, IRubyObject recv, IRubyObject filename) {
        Ruby runtime = context.runtime;
        RubyString path = StringSupport.checkEmbeddedNulls(runtime, get_path(context, filename));
        return runtime.newFileStat(path.getUnicodeValue(), true).ftype();
    }
    
    /*
     * Fixme:  This does not have exact same semantics as RubyArray.join, but they
     * probably could be consolidated (perhaps as join(args[], sep, doChomp)).
     */
    @JRubyMethod(rest = true, meta = true)
    public static RubyString join(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return join(context, recv, RubyArray.newArrayNoCopyLight(context.runtime, args));
    }
    
    @JRubyMethod(name = "lstat", required = 1, meta = true)
    public static IRubyObject lstat(ThreadContext context, IRubyObject recv, IRubyObject filename) {
        Ruby runtime = context.runtime;
        String f = StringSupport.checkEmbeddedNulls(runtime, get_path(context, filename)).getUnicodeValue();
        return runtime.newFileStat(f, true);
    }

    @JRubyMethod(name = "stat", required = 1, meta = true)
    public static IRubyObject stat(ThreadContext context, IRubyObject recv, IRubyObject filename) {
        Ruby runtime = context.runtime;
        String f = StringSupport.checkEmbeddedNulls(runtime, get_path(context, filename)).getUnicodeValue();
        return runtime.newFileStat(f, false);
    }

    @JRubyMethod(name = "atime", required = 1, meta = true)
    public static IRubyObject atime(ThreadContext context, IRubyObject recv, IRubyObject filename) {
        Ruby runtime = context.runtime;
        String f = StringSupport.checkEmbeddedNulls(runtime, get_path(context, filename)).getUnicodeValue();
        return runtime.newFileStat(f, false).atime();
    }

    @JRubyMethod(name = "ctime", required = 1, meta = true)
    public static IRubyObject ctime(ThreadContext context, IRubyObject recv, IRubyObject filename) {
        Ruby runtime = context.runtime;
        String f = StringSupport.checkEmbeddedNulls(runtime, get_path(context, filename)).getUnicodeValue();
        return runtime.newFileStat(f, false).ctime();
    }

    @JRubyMethod(name = "birthtime", required = 1, meta = true)
    public static IRubyObject birthtime(ThreadContext context, IRubyObject recv, IRubyObject filename) {
        Ruby runtime = context.runtime;
        String f = StringSupport.checkEmbeddedNulls(runtime, get_path(context, filename)).getUnicodeValue();
        return runtime.newFileStat(f, false).birthtime();
    }

    @JRubyMethod(required = 1, rest = true, meta = true)
    public static IRubyObject lchmod(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        
        int count = 0;
        RubyInteger mode = args[0].convertToInteger();
        for (int i = 1; i < args.length; i++) {
            JRubyFile file = file(args[i]);
            if (0 != runtime.getPosix().lchmod(file.toString(), (int)mode.getLongValue())) {
                throw runtime.newErrnoFromLastPOSIXErrno();
            } else {
                count++;
            }
        }
        
        return runtime.newFixnum(count);
    }

    @JRubyMethod(required = 2, rest = true, meta = true)
    public static IRubyObject lchown(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        int owner = !args[0].isNil() ? RubyNumeric.num2int(args[0]) : -1;
        int group = !args[1].isNil() ? RubyNumeric.num2int(args[1]) : -1;
        int count = 0;

        for (int i = 2; i < args.length; i++) {
            JRubyFile file = file(args[i]);

            if (0 != runtime.getPosix().lchown(file.toString(), owner, group)) {
                throw runtime.newErrnoFromLastPOSIXErrno();
            } else {
                count++;
            }
        }

        return runtime.newFixnum(count);
    }

    @JRubyMethod(required = 2, meta = true)
    public static IRubyObject link(ThreadContext context, IRubyObject recv, IRubyObject from, IRubyObject to) {
        Ruby runtime = context.runtime;
        String fromStr = file(from).toString();
        String toStr = file(to).toString();

        int ret = runtime.getPosix().link(fromStr, toStr);
        if (ret != 0) {
            if (runtime.getPosix().isNative()) {
                throw runtime.newErrnoFromInt(runtime.getPosix().errno(), String.format("(%s, %s)", fromStr, toStr));
            } else {
                // In most cases, when there is an error during the call,
                // the POSIX handler throws an exception, but not in case
                // with pure Java POSIX layer (when native support is disabled),
                // so we deal with it like this:
                throw runtime.newErrnoEEXISTError(fromStr + " or " + toStr);
            }
        }
        return runtime.newFixnum(ret);
    }

    @JRubyMethod(required = 1, meta = true)
    public static IRubyObject mtime(ThreadContext context, IRubyObject recv, IRubyObject filename) {
        Ruby runtime = context.runtime;
        String f = StringSupport.checkEmbeddedNulls(runtime, get_path(context, filename)).getUnicodeValue();
        return runtime.newFileStat(f, false).mtime();
    }
    
    @JRubyMethod(required = 2, meta = true)
    public static IRubyObject rename(ThreadContext context, IRubyObject recv, IRubyObject oldName, IRubyObject newName) {
        Ruby runtime = context.runtime;
        RubyString oldNameString = StringSupport.checkEmbeddedNulls(runtime, get_path(context, oldName));
        RubyString newNameString = StringSupport.checkEmbeddedNulls(runtime, get_path(context, newName));

        String newNameJavaString = newNameString.getUnicodeValue();
        String oldNameJavaString = oldNameString.getUnicodeValue();
        JRubyFile oldFile = JRubyFile.create(runtime.getCurrentDirectory(), oldNameJavaString);
        JRubyFile newFile = JRubyFile.create(runtime.getCurrentDirectory(), newNameJavaString);
        
        boolean isOldSymlink = RubyFileTest.symlink_p(recv, oldNameString).isTrue();
        // Broken symlinks considered by exists() as non-existing,
        // so we need to check for symlinks explicitly.
        if (!(oldFile.exists() || isOldSymlink) || !newFile.getParentFile().exists()) {
            throw runtime.newErrnoENOENTError(oldNameJavaString + " or " + newNameJavaString);
        }

        JRubyFile dest = JRubyFile.create(runtime.getCurrentDirectory(), newNameJavaString);

        if (oldFile.renameTo(dest)) {  // rename is successful
            return RubyFixnum.zero(runtime);
        }

        // rename via Java API call wasn't successful, let's try some tricks, similar to MRI 

        if (newFile.exists()) {
            runtime.getPosix().chmod(newNameJavaString, 0666);
            newFile.delete();
        }

        if (oldFile.renameTo(dest)) { // try to rename one more time
            return RubyFixnum.zero(runtime);
        }

        throw runtime.newErrnoEACCESError(oldNameJavaString + " or " + newNameJavaString);
    }
    
    @JRubyMethod(required = 1, meta = true)
    public static RubyArray split(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        Ruby runtime = context.runtime;
        RubyString filename = StringSupport.checkEmbeddedNulls(runtime, get_path(context, arg));

        return runtime.newArray(dirname(context, recv, filename),
                basename(context, recv, new IRubyObject[]{filename}));
    }
    
    @JRubyMethod(required = 2, meta = true)
    public static IRubyObject symlink(ThreadContext context, IRubyObject recv, IRubyObject from, IRubyObject to) {
        Ruby runtime = context.runtime;
        RubyString fromStr = StringSupport.checkEmbeddedNulls(runtime, get_path(context, from));
        RubyString toStr = StringSupport.checkEmbeddedNulls(runtime, get_path(context, to));
        String tovalue = toStr.getUnicodeValue();
        tovalue = JRubyFile.create(runtime.getCurrentDirectory(), tovalue).getAbsolutePath();
        try {
            if (runtime.getPosix().symlink(fromStr.getUnicodeValue(), tovalue) == -1) {
                if (runtime.getPosix().isNative()) {
                    throw runtime.newErrnoFromInt(runtime.getPosix().errno(), String.format("(%s, %s)", fromStr, toStr));
                } else {
                    throw runtime.newErrnoEEXISTError(String.format("(%s, %s)", fromStr, toStr));
                }
            }
        } catch (java.lang.UnsatisfiedLinkError ule) {
            throw runtime.newNotImplementedError("symlink() function is unimplemented on this machine");
        }
        
        return RubyFixnum.zero(runtime);
    }
    
    @JRubyMethod(required = 1, meta = true)
    public static IRubyObject readlink(ThreadContext context, IRubyObject recv, IRubyObject path) {
        Ruby runtime = context.runtime;
        JRubyFile link = file(path);

        try {
            String realPath = runtime.getPosix().readlink(link.toString());

            if (realPath == null) {
                throw runtime.newErrnoFromLastPOSIXErrno();
            }

            return runtime.newString(realPath);
        } catch (IOException e) {
            throw runtime.newIOError(e.getMessage());
        }
    }

    // Can we produce IOError which bypasses a close?
    public static IRubyObject truncate(ThreadContext context, IRubyObject recv, IRubyObject arg1, IRubyObject arg2) {        
        return truncate19(context, recv, arg1, arg2);
    }

    @JRubyMethod(name = "truncate", required = 2, meta = true)
    public static IRubyObject truncate19(ThreadContext context, IRubyObject recv, IRubyObject arg1, IRubyObject arg2) {
        RubyString path = StringSupport.checkEmbeddedNulls(context.runtime, get_path(context, arg1));
        return truncateCommon(context, recv, path, arg2);
    }

    @JRubyMethod(meta = true, optional = 1)
    public static IRubyObject umask(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        int oldMask = 0;
        if (args.length == 0) {
            oldMask = PosixShim.umask(runtime.getPosix());
        } else if (args.length == 1) {
            int newMask = (int) args[0].convertToInteger().getLongValue();
            oldMask = PosixShim.umask(runtime.getPosix(), newMask);
        } else {
            runtime.newArgumentError("wrong number of arguments");
        }
        
        return runtime.newFixnum(oldMask);
    }

    @JRubyMethod(required = 2, rest = true, meta = true)
    public static IRubyObject utime(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        long[] atimeval = null;
        long[] mtimeval = null;

        if (args[0] != runtime.getNil() || args[1] != runtime.getNil()) {
            atimeval = extractTimeval(runtime, args[0]);
            mtimeval = extractTimeval(runtime, args[1]);
        }

        for (int i = 2, j = args.length; i < j; i++) {
            RubyString filename = StringSupport.checkEmbeddedNulls(runtime, get_path(context, args[i]));
            
            JRubyFile fileToTouch = JRubyFile.create(runtime.getCurrentDirectory(),filename.getUnicodeValue());
            
            if (!fileToTouch.exists()) {
                throw runtime.newErrnoENOENTError(filename.toString());
            }

            int result = runtime.getPosix().utimes(fileToTouch.getAbsolutePath(), atimeval, mtimeval);
            if (result == -1) {
                throw runtime.newErrnoFromInt(runtime.getPosix().errno());
            }
        }
        
        return runtime.newFixnum(args.length - 2);
    }
    
    @JRubyMethod(rest = true, meta = true)
    public static IRubyObject delete(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = context.runtime;
         
        for (int i = 0; i < args.length; i++) {
            RubyString filename = StringSupport.checkEmbeddedNulls(runtime, get_path(context, args[i]));
            JRubyFile lToDelete = JRubyFile.create(runtime.getCurrentDirectory(), filename.getUnicodeValue());
            
            boolean isSymlink = RubyFileTest.symlink_p(recv, filename).isTrue();
            // Broken symlinks considered by exists() as non-existing,
            // so we need to check for symlinks explicitly.
            if (!lToDelete.exists() && !isSymlink) {
                throw runtime.newErrnoENOENTError(filename.getUnicodeValue());
            }

            if (lToDelete.isDirectory() && !isSymlink) {
                throw runtime.newErrnoEPERMError(filename.getUnicodeValue());
            }

            if (!lToDelete.delete()) {
                throw runtime.newErrnoEACCESError(filename.getUnicodeValue());
            }
        }
        
        return runtime.newFixnum(args.length);
    }

    @JRubyMethod(rest = true, meta = true)
    public static IRubyObject unlink(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        POSIX posix = runtime.getPosix();

        if (!posix.isNative() || Platform.IS_WINDOWS) return delete(context, recv, args);

        for (int i = 0; i < args.length; i++) {
            RubyString filename = StringSupport.checkEmbeddedNulls(runtime, get_path(context, args[i]));
            JRubyFile lToDelete = JRubyFile.create(runtime.getCurrentDirectory(), filename.getUnicodeValue());

            boolean isSymlink = RubyFileTest.symlink_p(recv, filename).isTrue();
            // Broken symlinks considered by exists() as non-existing,
            // so we need to check for symlinks explicitly.
            if (!lToDelete.exists() && !isSymlink) {
                throw runtime.newErrnoENOENTError(filename.getUnicodeValue());
            }

            if (lToDelete.isDirectory() && !isSymlink) {
                throw runtime.newErrnoEPERMError(filename.getUnicodeValue());
            }

            if (posix.unlink(lToDelete.getAbsolutePath()) < 0) {
                throw runtime.newErrnoFromInt(posix.errno());
            }
        }

        return runtime.newFixnum(args.length);
    }

    public static IRubyObject unlink(ThreadContext context, IRubyObject... args) {
        return unlink(context, context.runtime.getFile(), args);
    }

    // rb_file_size but not using stat
    @JRubyMethod
    public IRubyObject size(ThreadContext context) {
        Ruby runtime = context.runtime;
        OpenFile fptr;
        FileStat st;
        long size;

        fptr = getOpenFileChecked();
        if ((fptr.getMode() & OpenFile.WRITABLE) != 0) {
            flushRaw(context, false);
        }

        size = fptr.posix.size(fptr.fd());

        return RubyFixnum.newFixnum(runtime, size);
    }

    public String getPath() {
        if (openFile == null) return null;
        return openFile.getPath();
    }

    private void setPath(String path) {
        if (openFile == null) return;
        openFile.setPath(path);
    }

    @Override
    public Encoding getEncoding() {
        return null;
    }

    @Override
    public void setEncoding(Encoding encoding) {
        // :)
    }

    // mri: rb_open_file + rb_scan_open_args
    protected IRubyObject openFile(ThreadContext context, IRubyObject args[]) {
        Ruby runtime = context.runtime;
        RubyString filename = StringSupport.checkEmbeddedNulls(runtime, get_path(context, args[0]));

        setPath(adjustRootPathOnWindows(runtime, filename.asJavaString(), runtime.getCurrentDirectory()));

        Object pm = EncodingUtils.vmodeVperm(null, null);
        IRubyObject options = context.nil;
        
        switch(args.length) {
            case 1:
                break;
            case 2: {
                IRubyObject test = TypeConverter.checkHashType(runtime, args[1]);
                if (test instanceof RubyHash) {
                    options = test;
                } else {
                    vmode(pm, args[1]);
                }
                break;
            }
            case 3: {
                IRubyObject test = TypeConverter.checkHashType(runtime, args[2]);
                if (test instanceof RubyHash) {
                    options = test;
                } else {
                    vperm(pm, args[2]);
                }
                vmode(pm, args[1]);
                break;
            }
            case 4:
                options = args[3].convertToHash();
                vperm(pm, args[2]);
                vmode(pm, args[1]);
                break;
        }
        
        int[] oflags_p = {0}, fmode_p = {0};
        IOEncodable convconfig = new ConvConfig();
        EncodingUtils.extractModeEncoding(context, convconfig, pm, options, oflags_p, fmode_p);
        int perm = (vperm(pm) != null && !vperm(pm).isNil()) ? 
                RubyNumeric.num2int(vperm(pm)) : 0666;
        
        return fileOpenGeneric(context, filename, oflags_p[0], fmode_p[0], convconfig, perm);
    }
    
    // rb_file_open_generic
    public IRubyObject fileOpenGeneric(ThreadContext context, IRubyObject filename, int oflags, int fmode, IOEncodable convConfig, int perm) {
        if (convConfig == null) {
            convConfig = new ConvConfig();
            EncodingUtils.ioExtIntToEncs(context, convConfig, null, null, fmode);
            convConfig.setEcflags(0);
            convConfig.setEcopts(context.nil);
        }
        
        int[] fmode_p = {fmode};
        
        EncodingUtils.validateEncodingBinmode(context, fmode_p, convConfig.getEcflags(), convConfig);
        
        OpenFile fptr = MakeOpenFile();

        fptr.setMode(fmode_p[0]);
        fptr.encs.copy(convConfig);
        fptr.setPath(RubyFile.get_path(context, filename).asJavaString());

        fptr.setFD(sysopen(context.runtime, fptr.getPath(), oflags, perm));
        fptr.checkTTY();
        if ((fmode_p[0] & OpenFile.SETENC_BY_BOM) != 0) {
            EncodingUtils.ioSetEncodingByBOM(context, this);
        }

        return this;
    }

    // mri: FilePathValue/rb_get_path/rb_get_patch_check
    public static RubyString get_path(ThreadContext context, IRubyObject path) {
        if (path instanceof RubyString) return (RubyString) path;

        if (path.respondsTo("to_path")) path = path.callMethod(context, "to_path");

        return filePathConvert(context, path.convertToString());
    }
    
    // FIXME: MRI skips this logic on windows?  Does not make sense to me why so I left it in.
    // mri: file_path_convert
    private static RubyString filePathConvert(ThreadContext context, RubyString path) {
        if (!org.jruby.platform.Platform.IS_WINDOWS) {
            Ruby runtime = context.getRuntime();
            EncodingService encodingService = runtime.getEncodingService();
            Encoding pathEncoding = path.getEncoding();

            // If we are not ascii and do not match fs encoding then transcode to fs.
            if (runtime.getDefaultInternalEncoding() != null &&
                    pathEncoding != encodingService.getUSAsciiEncoding() &&
                    pathEncoding != encodingService.getAscii8bitEncoding() &&
                    pathEncoding != encodingService.getFileSystemEncoding(runtime) &&
                    !path.isAsciiOnly()) {
                ByteList bytes = EncodingUtils.strConvEnc(context, path.getByteList(), pathEncoding, encodingService.getFileSystemEncoding(runtime));
                path = RubyString.newString(runtime, bytes);
            }
        }

        return path;
    }


    public static FileResource fileResource(ThreadContext context, IRubyObject pathOrFile) {
        Ruby runtime = context.runtime;

        if (pathOrFile instanceof RubyFile) {
            return JRubyFile.createResource(runtime, ((RubyFile) pathOrFile).getPath());
        } else if (pathOrFile instanceof RubyIO) {
            return JRubyFile.createResource(runtime, ((RubyIO) pathOrFile).openFile.getPath());

        }

        RubyString path = StringSupport.checkEmbeddedNulls(runtime, get_path(context, pathOrFile));
        return JRubyFile.createResource(runtime, path.toString());
    }
    /**
     * Get the fully-qualified JRubyFile object for the path, taking into
     * account the runtime's current directory.
     */
    public static FileResource fileResource(IRubyObject pathOrFile) {
        Ruby runtime = pathOrFile.getRuntime();

        if (pathOrFile instanceof RubyFile) {
            return JRubyFile.createResource(runtime, ((RubyFile) pathOrFile).getPath());
        } else if (pathOrFile instanceof RubyIO) {
            return JRubyFile.createResource(runtime, ((RubyIO) pathOrFile).openFile.getPath());
        }

        ThreadContext context = runtime.getCurrentContext();
        RubyString path = StringSupport.checkEmbeddedNulls(runtime, get_path(context, pathOrFile));
        return JRubyFile.createResource(runtime, path.toString());
    }

    @Deprecated // Use fileResource instead
    public static JRubyFile file(IRubyObject pathOrFile) {
      return fileResource(pathOrFile).hackyGetJRubyFile();
    }

    @Override
    public String toString() {
        return "RubyFile(" + openFile.getPath() + ", " + openFile.getMode();
    }

    public static ZipEntry getFileEntry(ZipFile zf, String path) throws IOException {
        ZipEntry entry = zf.getEntry(path);
        if (entry == null) {
            // try canonicalizing the path to eliminate . and .. (JRUBY-4760, JRUBY-4879)
            String prefix = new File(".").getCanonicalPath();
            entry = zf.getEntry(new File(path).getCanonicalPath().substring(prefix.length() + 1).replaceAll("\\\\", "/"));
        }
        return entry;
    }
    
    public static ZipEntry getDirOrFileEntry(String jar, String path) throws IOException {
        return getDirOrFileEntry(new JarFile(jar), path);
    }    
    
    public static ZipEntry getDirOrFileEntry(ZipFile zf, String path) throws IOException {
        String dirPath = path + "/";
        ZipEntry entry = zf.getEntry(dirPath); // first try as directory
        if (entry == null) {
            if (dirPath.length() == 1) {
                return new ZipEntry(dirPath);
            }
            // try canonicalizing the path to eliminate . and .. (JRUBY-4760, JRUBY-4879)
            String prefix = new File(".").getCanonicalPath();
            entry = zf.getEntry(new File(dirPath).getCanonicalPath().substring(prefix.length() + 1).replaceAll("\\\\", "/"));

            // JRUBY-6119
            if (entry == null) {
                Enumeration<? extends ZipEntry> entries = zf.entries();
                while (entries.hasMoreElements()) {
                    String zipEntry = entries.nextElement().getName();
                    if (zipEntry.startsWith(dirPath)) {
                        return new ZipEntry(dirPath);
                    }
                }
            }

            if (entry == null) {
                // try as file
                entry = getFileEntry(zf, path);
            }
        }
        return entry;
    }
    
    // mri: rb_is_absolute_path
    // Do this versus stand up full JRubyFile and perform stats + canonicalization
    private static boolean isAbsolutePath(String path) {
        return (path != null && path.length() > 1 && path.charAt(0) == '/') ||
                startsWithDriveLetterOnWindows(path);
    }

    public static boolean startsWithDriveLetterOnWindows(String path) {
        return (path != null)
                && Platform.IS_WINDOWS &&
                ((path.length()>1 && path.charAt(0) == '/') ?
                        (path.length() > 2
                                && isWindowsDriveLetter(path.charAt(1))
                                && path.charAt(2) == ':') :
                        (path.length() > 1
                                && isWindowsDriveLetter(path.charAt(0))
                                && path.charAt(1) == ':'));
    }
    
    // adjusts paths started with '/' or '\\', on windows.
    static String adjustRootPathOnWindows(Ruby runtime, String path, String dir) {
        if (path == null || !Platform.IS_WINDOWS) return path;

        // MRI behavior on Windows: it treats '/' as a root of
        // a current drive (but only if SINGLE slash is present!):
        // E.g., if current work directory is
        // 'D:/home/directory', then '/' means 'D:/'.
        //
        // Basically, '/path' is treated as a *RELATIVE* path,
        // relative to the current drive. '//path' is treated
        // as absolute one.
        if ((path.startsWith("/") && !(path.length() > 2 && path.charAt(2) == ':')) || path.startsWith("\\")) {
            if (path.length() > 1 && (path.charAt(1) == '/' || path.charAt(1) == '\\')) {
                return path;
            }

            // First try to use drive letter from supplied dir value,
            // then try current work dir.
            if (!startsWithDriveLetterOnWindows(dir)) {
                dir = runtime.getCurrentDirectory();
            }
            if (dir.length() >= 2) {
                path = dir.substring(0, 2) + path;
            }
        } else if (startsWithDriveLetterOnWindows(path) && path.length() == 2) {
            // compensate for missing slash after drive letter on windows
            path += "/";
        }

        return path;
    }

    /**
     * Extract a timeval (an array of 2 longs: seconds and microseconds from epoch) from
     * an IRubyObject.
     */
    private static long[] extractTimeval(Ruby runtime, IRubyObject value) {
        long[] timeval = new long[2];

        if (value instanceof RubyFloat) {
            timeval[0] = Platform.IS_32_BIT ? RubyNumeric.num2int(value) : RubyNumeric.num2long(value);
            double fraction = ((RubyFloat) value).getDoubleValue() % 1.0;
            timeval[1] = (long)(fraction * 1e6 + 0.5);
        } else if (value instanceof RubyNumeric) {
            timeval[0] = Platform.IS_32_BIT ? RubyNumeric.num2int(value) : RubyNumeric.num2long(value);
            timeval[1] = 0;
        } else {
            RubyTime time;
            if (value instanceof RubyTime) {
                time = ((RubyTime) value);
            } else {
                time = (RubyTime) TypeConverter.convertToType(value, runtime.getTime(), "to_time", true);
            }
            timeval[0] = Platform.IS_32_BIT ? RubyNumeric.num2int(time.to_i()) : RubyNumeric.num2long(time.to_i());
            timeval[1] = Platform.IS_32_BIT ? RubyNumeric.num2int(time.usec()) : RubyNumeric.num2long(time.usec());
        }

        return timeval;
    }

    private void checkClosed(ThreadContext context) {
        openFile.checkClosed();
    }

    private static boolean isWindowsDriveLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private static IRubyObject expandPathInternal(ThreadContext context, IRubyObject recv, IRubyObject[] args, boolean expandUser, boolean canonicalize) {
        Ruby runtime = context.runtime;

        RubyString origPath = StringSupport.checkEmbeddedNulls(runtime, get_path(context, args[0]));
        String relativePath = origPath.getUnicodeValue();

        // Special /dev/null of windows
        if (Platform.IS_WINDOWS && ("NUL:".equalsIgnoreCase(relativePath) || "NUL".equalsIgnoreCase(relativePath))) {
            return runtime.newString("//./" + relativePath.substring(0, 3));
        }

        if (relativePath.startsWith("uri:")) {
            return runtime.newString(relativePath);
        }

        String[] uriParts = splitURI(relativePath);
        String cwd;

        // Handle ~user paths
        if (expandUser) {
            relativePath = expandUserPath(context, relativePath, true);
        }

        if (uriParts != null) {
            //If the path was an absolute classpath path, return it as-is.
            if (uriParts[0].equals("classpath:")) {
                return runtime.newString(relativePath);
            }

            relativePath = uriParts[1];
        }

        // Now that we're not treating it as a URI, we need to honor the canonicalize flag.
        // Do not insert early returns below.

        // If there's a second argument, it's the path to which the first
        // argument is relative.
        if (args.length == 2 && !args[1].isNil()) {
            // TODO maybe combine this with get_path method
            if ((args[1] instanceof RubyString) && args[1].asJavaString().startsWith("uri:")) {
                cwd = args[1].asJavaString();
            } else {
                cwd = StringSupport.checkEmbeddedNulls(runtime, get_path(context, args[1])).getUnicodeValue();
    
                // Handle ~user paths.
                if (expandUser) {
                    cwd = expandUserPath(context, cwd, true);
                }
    
                String[] cwdURIParts = splitURI(cwd);
                if (uriParts == null && cwdURIParts != null) {
                    uriParts = cwdURIParts;
                    cwd = cwdURIParts[1];
                }
    
                cwd = adjustRootPathOnWindows(runtime, cwd, null);
    
                boolean startsWithSlashNotOnWindows = (cwd != null)
                        && !Platform.IS_WINDOWS && cwd.length() > 0
                        && cwd.charAt(0) == '/';
    
                // TODO: better detection when path is absolute or not.
                // If the path isn't absolute, then prepend the current working
                // directory to the path.
                if (!startsWithSlashNotOnWindows && !startsWithDriveLetterOnWindows(cwd)) {
                    cwd = new File(runtime.getCurrentDirectory(), cwd).getAbsolutePath();
                }
            }
        } else {
            // If there's no second argument, simply use the working directory
            // of the runtime.
            cwd = runtime.getCurrentDirectory();
        }

        // Something wrong we don't know the cwd...
        // TODO: Is this behavior really desirable? /mov
        if (cwd == null) return runtime.getNil();

        /* The counting of slashes that follows is simply a way to adhere to
        * Ruby's UNC (or something) compatibility. When Ruby's expand_path is
        * called with "//foo//bar" it will return "//foo/bar". JRuby uses
        * java.io.File, and hence returns "/foo/bar". In order to retain
        * java.io.File in the lower layers and provide full Ruby
        * compatibility, the number of extra slashes must be counted and
        * prepended to the result.
        */

        // TODO: special handling on windows for some corner cases
//        if (IS_WINDOWS) {
//            if (relativePath.startsWith("//")) {
//                if (relativePath.length() > 2 && relativePath.charAt(2) != '/') {
//                    int nextSlash = relativePath.indexOf('/', 3);
//                    if (nextSlash != -1) {
//                        return runtime.newString(
//                                relativePath.substring(0, nextSlash)
//                                + canonicalize(relativePath.substring(nextSlash)));
//                    } else {
//                        return runtime.newString(relativePath);
//                    }
//                }
//            }
//        }

        // Find out which string to check.
        String padSlashes = "";
        if (uriParts != null) {
            padSlashes = uriParts[0];
        } else if (!Platform.IS_WINDOWS) {
            if (relativePath.length() > 0 && relativePath.charAt(0) == '/') {
                padSlashes = countSlashes(relativePath);
            } else if (cwd.length() > 0 && cwd.charAt(0) == '/') {
                padSlashes = countSlashes(cwd);
            }
        }

        JRubyFile path;

        if (relativePath.length() == 0) {
            path = JRubyFile.create(relativePath, cwd);
        } else {
            relativePath = adjustRootPathOnWindows(runtime, relativePath, cwd);
            path = JRubyFile.create(cwd, relativePath);
        }

        String realPath = padSlashes + canonicalize(path.getAbsolutePath());

        if (canonicalize) {
            try {
                realPath = new File(realPath).getCanonicalPath();
            } catch (IOException ioe) {
                // Earlier canonicalization will have to do.
            }
        }

        return runtime.newString(realPath);
    }

    public static String[] splitURI(String path) {
        Matcher m = URI_PREFIX.matcher(path);
        if (m.find()) {
            if (m.group(2).length() == 0) {
                return new String[] {path, ""};
            }
            String pathWithoutJarPrefix;
            if (m.group(1) != null) {
                pathWithoutJarPrefix = path.substring(4);
            } else {
                pathWithoutJarPrefix = path;
            }
            try {
                URI u = new URI(pathWithoutJarPrefix);
                String pathPart = u.getPath();
                return new String[] {path.substring(0, path.indexOf(pathPart)), pathPart};
            } catch (Exception e) {
                try {
                    URL u = new URL(pathWithoutJarPrefix);
                    String pathPart = u.getPath();
                    return new String[] {path.substring(0, path.indexOf(pathPart)), pathPart};
                } catch (Exception e2) {
                }
            }
        }
        return null;
    }

    /**
     * This method checks a path, and if it starts with ~, then it expands
     * the path to the absolute path of the user's home directory. If the
     * string does not begin with ~, then the string is simply returned.
     * unaltered.
     * @param context
     * @param path Path to check
     * @return Expanded path
     */
    public static String expandUserPath(ThreadContext context, String path) {
        return expandUserPath(context, path, false);
    }
    
    // FIXME: The variations of expand* and need for each to have a boolean discriminator makes
    // this code ripe for refactoring...
    public static String expandUserPath(ThreadContext context, String path, boolean raiseOnRelativePath) {
        int pathLength = path.length();

        if (pathLength >= 1 && path.charAt(0) == '~') {
            // Enebo : Should ~frogger\\foo work (it doesnt in linux ruby)?
            int userEnd = path.indexOf('/');

            if (userEnd == -1) {
                if (pathLength == 1) {
                    // Single '~' as whole path to expand
                    checkHome(context);
                    path = RubyDir.getHomeDirectoryPath(context).toString();
                    
                    if (raiseOnRelativePath && !isAbsolutePath(path)) throw context.runtime.newArgumentError("non-absolute home");
                } else {
                    // No directory delimeter.  Rest of string is username
                    userEnd = pathLength;
                }
            }

            if (userEnd == 1) {
                // '~/...' as path to expand
                checkHome(context);
                path = RubyDir.getHomeDirectoryPath(context).toString() +
                        path.substring(1);
                
                if (raiseOnRelativePath && !isAbsolutePath(path)) throw context.runtime.newArgumentError("non-absolute home");
            } else if (userEnd > 1){
                // '~user/...' as path to expand
                String user = path.substring(1, userEnd);
                IRubyObject dir = RubyDir.getHomeDirectoryPath(context, user);

                if (dir.isNil()) {
                    throw context.runtime.newArgumentError("user " + user + " does not exist");
                }

                path = "" + dir + (pathLength == userEnd ? "" : path.substring(userEnd));
                
                // getpwd (or /etc/passwd fallback) returns a home which is not absolute!!! [mecha-unlikely]
                if (raiseOnRelativePath && !isAbsolutePath(path)) throw context.runtime.newArgumentError("non-absolute home of " + user);
            }
        }
        return path;
    }

    /**
     * Returns a string consisting of <code>n-1</code> slashes, where
     * <code>n</code> is the number of slashes at the beginning of the input
     * string.
     * @param stringToCheck
     * @return
     */
    private static String countSlashes( String stringToCheck ) {
        // Count number of extra slashes in the beginning of the string.
        int slashCount = 0;
        for (int i = 0; i < stringToCheck.length(); i++) {
            if (stringToCheck.charAt(i) == '/') {
                slashCount++;
            } else {
                break;
            }
        }

        // If there are N slashes, then we want N-1.
        if (slashCount > 0) {
            slashCount--;
        }

        if (slashCount < SLASHES.length) {
            return SLASHES[slashCount];
        }

        // Prepare a string with the same number of redundant slashes so that
        // we easily can prepend it to the result.
        char[] slashes = new char[slashCount];
        for (int i = 0; i < slashCount; i++) {
            slashes[i] = '/';
        }
        return new String(slashes);
    }

    public static String canonicalize(String path) {
        return canonicalize(null, path);
    }

    private static String canonicalize(String canonicalPath, String remaining) {
        if (remaining == null) {
            if ("".equals(canonicalPath)) {
                return "/";
            } else {
                // compensate for missing slash after drive letter on windows
                if (startsWithDriveLetterOnWindows(canonicalPath)
                        && canonicalPath.length() == 2) {
                    canonicalPath += "/";
                }
            }
            return canonicalPath;
        }

        String child;
        int slash = remaining.indexOf('/');
        if (slash == -1) {
            child = remaining;
            remaining = null;
        } else {
            child = remaining.substring(0, slash);
            remaining = remaining.substring(slash + 1);
        }

        if (child.equals(".")) {
            // no canonical path yet or length is zero, and we have a / followed by a dot...
            if (slash == -1) {
                // we don't have another slash after this, so replace /. with /
                if (canonicalPath != null && canonicalPath.length() == 0 && slash == -1) canonicalPath += "/";
            } else {
                // we do have another slash; omit both / and . (JRUBY-1606)
            }
        } else if (child.equals("..")) {
            if (canonicalPath == null) throw new IllegalArgumentException("Cannot have .. at the start of an absolute path");
            int lastDir = canonicalPath.lastIndexOf('/');
            if (lastDir == -1) {
                if (startsWithDriveLetterOnWindows(canonicalPath)) {
                    // do nothing, we should not delete the drive letter
                } else {
                    canonicalPath = "";
                }
            } else {
                canonicalPath = canonicalPath.substring(0, lastDir);
            }
        } else if (canonicalPath == null) {
            canonicalPath = child;
        } else {
            canonicalPath += "/" + child;
        }

        return canonicalize(canonicalPath, remaining);
    }
    
    /**
     * Check if HOME environment variable is not nil nor empty
     * @param context 
     */
    private static void checkHome(ThreadContext context) {
        Ruby runtime = context.runtime;
        RubyHash env = runtime.getENV();
        String home = (String) env.get(runtime.newString("HOME"));
        if (home == null || home.equals("")) {
            throw runtime.newArgumentError("couldn't find HOME environment -- expanding `~'");
        }
    }

    private static String inspectJoin(ThreadContext context, IRubyObject recv, RubyArray parent, RubyArray array) {
        Ruby runtime = context.runtime;

        // If already inspecting, there is no need to register/unregister again.
        if (runtime.isInspecting(parent)) return join(context, recv, array).toString();

        try {
            runtime.registerInspecting(parent);
            return join(context, recv, array).toString();
        } finally {
            runtime.unregisterInspecting(parent);
        }
    }

    private static RubyString join(ThreadContext context, IRubyObject recv, RubyArray ary) {
        IRubyObject[] args = ary.toJavaArray();
        boolean isTainted = false;
        StringBuilder buffer = new StringBuilder();
        Ruby runtime = context.runtime;
        String separator = context.getRuntime().getClass("File").getConstant("SEPARATOR").toString();

        for (int i = 0; i < args.length; i++) {
            if (args[i].isTaint()) {
                isTainted = true;
            }
            String element;
            if (args[i] instanceof RubyString) {
                element = args[i].convertToString().getUnicodeValue();
            } else if (args[i] instanceof RubyArray) {
                if (runtime.isInspecting(args[i])) {
                    throw runtime.newArgumentError("recursive array");
                } else {
                    element = inspectJoin(context, recv, ary, ((RubyArray)args[i]));
                }
            } else {
                RubyString path = StringSupport.checkEmbeddedNulls(runtime, get_path(context, args[i]));
                element = path.getUnicodeValue();
            }

            chomp(buffer);
            if (i > 0 && !element.startsWith(separator)) {
                buffer.append(separator);
            }
            buffer.append(element);
        }

        RubyString fixedStr = RubyString.newString(runtime, buffer.toString());
        fixedStr.setTaint(isTainted);
        return fixedStr;
    }

    private static void chomp(StringBuilder buffer) {
        int lastIndex = buffer.length() - 1;

        while (lastIndex >= 0 && (buffer.lastIndexOf("/") == lastIndex || buffer.lastIndexOf("\\") == lastIndex)) {
            buffer.setLength(lastIndex);
            lastIndex--;
        }
    }

    private static IRubyObject truncateCommon(ThreadContext context, IRubyObject recv, IRubyObject arg1, IRubyObject arg2) {
        RubyString filename = arg1.convertToString(); // TODO: SafeStringValue here
        Ruby runtime = context.runtime;
        RubyInteger newLength = arg2.convertToInteger();

        File testFile ;
        File childFile = new File(filename.getUnicodeValue() );
        String filenameString = Helpers.decodeByteList(runtime, filename.getByteList());

        if ( childFile.isAbsolute() ) {
            testFile = childFile ;
        } else {
            testFile = new File(runtime.getCurrentDirectory(), filenameString);
        }

        if (!testFile.exists()) {
            throw runtime.newErrnoENOENTError(filenameString);
        }

        if (newLength.getLongValue() < 0) {
            throw runtime.newErrnoEINVALError(filenameString);
        }

        IRubyObject[] args = new IRubyObject[] { filename, runtime.newString("r+") };
        RubyFile file = (RubyFile) open(context, recv, args, Block.NULL_BLOCK);
        file.truncate(context, newLength);
        file.close();

        return RubyFixnum.zero(runtime);
    }

    @Deprecated
    public IRubyObject initialize19(IRubyObject[] args, Block block) {
        return initialize(null, args, block);
    }

    private static final long serialVersionUID = 1L;

    public static final int LOCK_SH = PosixShim.LOCK_SH;
    public static final int LOCK_EX = PosixShim.LOCK_EX;
    public static final int LOCK_NB = PosixShim.LOCK_NB;
    public static final int LOCK_UN = PosixShim.LOCK_UN;

    private static final int FNM_NOESCAPE = 1;
    private static final int FNM_PATHNAME = 2;
    private static final int FNM_DOTMATCH = 4;
    private static final int FNM_CASEFOLD = 8;
    private static final int FNM_EXTGLOB = 16;
    private static final int FNM_SYSCASE = Platform.IS_WINDOWS ? FNM_CASEFOLD : 0;

    private static final String[] SLASHES = {"", "/", "//"};
    private static Pattern URI_PREFIX = Pattern.compile("^(jar:)?[a-z]{2,}:(.*)");

    @Deprecated
    protected String path;
}
