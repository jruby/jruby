/*
 ***** BEGIN LICENSE BLOCK *****
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
import jnr.posix.util.Platform;
import org.jcodings.Encoding;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.NotImplementedError;
import org.jruby.runtime.*;
import org.jruby.runtime.JavaSites.FileSites;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.encoding.EncodingCapable;
import org.jruby.runtime.encoding.EncodingService;
import org.jruby.util.*;
import org.jruby.util.io.EncodingUtils;
import org.jruby.util.io.IOEncodable;
import org.jruby.util.io.ModeFlags;
import org.jruby.util.io.OpenFile;
import org.jruby.util.io.PosixShim;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFileAttributeView;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.jruby.RubyInteger.singleCharByteList;
import static org.jruby.runtime.Visibility.PRIVATE;
import static org.jruby.util.StringSupport.*;
import static org.jruby.util.io.EncodingUtils.vmode;
import static org.jruby.util.io.EncodingUtils.vperm;

/**
 * The Ruby File class.
 */
@JRubyClass(name="File", parent="IO", include="FileTest")
public class RubyFile extends RubyIO implements EncodingCapable {

    static final ByteList SLASH = singleCharByteList((byte) '/');
    static final ByteList BACKSLASH = singleCharByteList((byte) '\\');

    public static RubyClass createFileClass(Ruby runtime) {
        ThreadContext context = runtime.getCurrentContext();

        RubyClass fileClass = runtime.defineClass("File", runtime.getIO(), RubyFile::new);

        fileClass.defineAnnotatedMethods(RubyFile.class);

        fileClass.setClassIndex(ClassIndex.FILE);
        fileClass.setReifiedClass(RubyFile.class);

        fileClass.kindOf = new RubyModule.JavaClassKindOf(RubyFile.class);

        // file separator constants
        RubyString separator = RubyString.newString(runtime, SLASH);
        separator.freeze(context);
        fileClass.defineConstant("SEPARATOR", separator);
        fileClass.defineConstant("Separator", separator);

        if (File.separatorChar == '\\') {
            RubyString altSeparator = RubyString.newString(runtime, BACKSLASH);
            altSeparator.freeze(context);
            fileClass.defineConstant("ALT_SEPARATOR", altSeparator);
        } else {
            fileClass.defineConstant("ALT_SEPARATOR", context.nil);
        }

        // path separator
        RubyString pathSeparator = RubyString.newString(runtime, singleCharByteList((byte) File.pathSeparatorChar));
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

        constants.setConstant("SHARE_DELETE", runtime.newFixnum(ModeFlags.SHARE_DELETE));

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
        if (OpenFlags.O_TMPFILE.defined()) {
            /* Create an unnamed temporary file */
            constants.setConstant("TMPFILE", runtime.newFixnum(OpenFlags.O_TMPFILE.intValue()));
        }

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

        if (Platform.IS_WINDOWS) {
            // readlink is not available on Windows. See below and jruby/jruby#3287.
            // TODO: MRI does not implement readlink on Windows, but perhaps we could?
            fileClass.searchMethod("readlink").setNotImplemented(true);

            fileClass.searchMethod("mkfifo").setNotImplemented(true);
        }

        if (!Platform.IS_BSD) {
            // lchmod appears to be mostly a BSD-ism, not supported on Linux.
            // See https://github.com/jruby/jruby/issues/5547
            fileClass.getSingletonClass().searchMethod("lchmod").setNotImplemented(true);
        }

        return fileClass;
    }

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
    RubyFile(Ruby runtime, String path, final Reader reader) {
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

    public RubyFile(Ruby runtime, String path, FileChannel channel) {
        super(runtime, runtime.getFile(), channel);
        this.setPath(path);
    }

    @Override
    protected IRubyObject rbIoClose(ThreadContext context) {
        // Make sure any existing lock is released before we try and close the file
        if (openFile.currentLock != null) {
            try {
                openFile.currentLock.release();
            } catch (IOException e) {
                throw context.runtime.newIOError(e.getMessage());
            }
        }
        return super.rbIoClose(context);
    }

    @JRubyMethod(required = 1)
    public IRubyObject flock(ThreadContext context, IRubyObject operation) {

        // Solaris uses a ruby-ffi version defined in jruby/kernel/file.rb, so re-dispatch
        if (Platform.IS_SOLARIS) {
            return callMethod(context, "flock", operation);
        }

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
    @JRubyMethod(name = "initialize", required = 1, optional = 3, visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject[] args, Block block) {
        if (openFile != null) {
            throw context.runtime.newRuntimeError("reinitializing File");
        }

        if (args.length > 0 && args.length <= 3) {
            IRubyObject fd = TypeConverter.convertToTypeWithCheck(context, args[0], context.runtime.getFixnum(), sites(context).to_int_checked);
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

        final String path = getPath();
        if ( ! new File(path).exists() ) {
            throw context.runtime.newErrnoENOENTError(path);
        }

        return context.runtime.newFixnum(context.runtime.getPosix().chmod(path, mode));
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

        final String path = getPath();
        if ( ! new File(path).exists() ) {
            throw context.runtime.newErrnoENOENTError(path);
        }

        return context.runtime.newFixnum(context.runtime.getPosix().chown(path, owner, group));
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
        return ((RubyFileStat) stat(context)).mtime();
    }

    @JRubyMethod(meta = true)
    public static IRubyObject path(ThreadContext context, IRubyObject self, IRubyObject str) {
        return StringSupport.checkEmbeddedNulls(context.runtime, get_path(context, str));
    }

    @JRubyMethod(name = {"path", "to_path"})
    public IRubyObject path(ThreadContext context) {
        if ((openFile.getMode() & OpenFile.TMPFILE) != 0) {
            throw context.runtime.newIOError("File is unnamed (TMPFILE?)");
        }

        final String path = getPath();
        if (path != null) {
            RubyString newPath = context.runtime.newString(path);
            newPath.setTaint(true);
            return newPath;
        }
        return context.nil;
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
            throw runtime.newErrnoFromErrno(fptr.posix.getErrno(), fptr.getPath());
        }

        return RubyFixnum.zero(runtime);
    }

    @Override
    public final IRubyObject inspect() {
        return inspect(metaClass.runtime.getCurrentContext());
    }

    @JRubyMethod
    public RubyString inspect(ThreadContext context) {
        final String path = openFile.getPath();
        ByteList str = new ByteList((path == null ? 4 : path.length()) + 8);

        str.append('#').append('<');
        str.append(getMetaClass().getRealClass().to_s().getByteList());
        str.append(':').append(path == null ? RubyNil.nilBytes : RubyEncoding.encodeUTF8(path));
        if (!openFile.isOpen()) {
            str.append(' ').append('(');
            str.append('c').append('l').append('o').append('s').append('e').append('d');
            str.append(')');
        }
        str.append('>');
        // MRI knows whether path is UTF-8 so it might return ASCII-8BIT (we do not check)
        str.setEncoding(UTF8Encoding.INSTANCE);
        return RubyString.newStringLight(context.runtime, str);
    }

    private static final String URI_PREFIX_STRING = "^(uri|jar|file|classpath):([^:/]{2,}:([^:/]{2,}:)?)?";
    private static final Pattern ROOT_PATTERN = Pattern.compile(URI_PREFIX_STRING + "/?/?$");

    private static final int NULL_CHAR = '\0';

    /* File class methods */

    @JRubyMethod(meta = true) // required = 1, optional = 1
    public static RubyString basename(ThreadContext context, IRubyObject recv, IRubyObject path) {
        return basenameImpl(context, (RubyClass) recv, path, null);
    }

    @JRubyMethod(meta = true) // required = 1, optional = 1
    public static RubyString basename(ThreadContext context, IRubyObject recv, IRubyObject path, IRubyObject ext) {
        return basenameImpl(context, (RubyClass) recv, path, ext == context.nil ? null : ext);
    }

    @Deprecated
    public static IRubyObject basename(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        IRubyObject ext = (args.length > 1 && args[1] != context.nil) ? args[1] : null;
        return basenameImpl(context, (RubyClass) recv, args[0], ext);
    }

    private static RubyString basenameImpl(ThreadContext context, RubyClass klass, IRubyObject path, IRubyObject ext) {
        final Ruby runtime = context.runtime;

        final int separatorChar = getSeparatorChar(klass);
        final int altSeparatorChar = getAltSeparatorChar(klass);

        RubyString origString = StringSupport.checkEmbeddedNulls(runtime, get_path(context, path));
        Encoding origEncoding = origString.getEncoding();
        String name = origString.toString();

        // uri-like paths without parent directory
        if (name.endsWith(".jar!/") || ROOT_PATTERN.matcher(name).matches()) return (RubyString) path;

        // MRI-compatible basename handling for windows drive letter paths
        if (Platform.IS_WINDOWS) {
            if (name.length() > 1 && name.charAt(1) == ':' && Character.isLetter(name.charAt(0))) {
                switch (name.length()) {
                case 2:
                    return (RubyString) RubyString.newEmptyString(runtime, origString.getEncoding()).infectBy(path);
                case 3:
                    return RubyString.newString(runtime, RubyString.encodeBytelist(name.substring(2), origEncoding));
                default:
                    switch (name.charAt(2)) {
                    case '/':
                    case '\\':
                        break;
                    default:
                        // strip c: away from relative-pathed name
                        name = name.substring(2);
                    }
                    break;
                }
            }
        }

        while (name.length() > 1 && (name.charAt(name.length() - 1) == separatorChar || (name.charAt(name.length() - 1) == altSeparatorChar))) {
            name = name.substring(0, name.length() - 1);
        }

        // Paths which end in File::SEPARATOR or File::ALT_SEPARATOR must be stripped off.
        int slashCount = 0;
        int length = name.length();
        for (int i = length - 1; i >= 0; i--) {
            char c = name.charAt(i);
            if (c != separatorChar && c != altSeparatorChar) {
                break;
            }
            slashCount++;
        }
        if (slashCount > 0 && length > 1) {
            name = name.substring(0, name.length() - slashCount);
        }

        int index = name.lastIndexOf(separatorChar);
        if (altSeparatorChar != NULL_CHAR) {
            index = Math.max(index, name.lastIndexOf(altSeparatorChar));
        }

        if (!(contentEquals(name, separatorChar) || (contentEquals(name, altSeparatorChar))) && index != -1) {
            name = name.substring(index + 1);
        }

        if (ext != null) {
            final String extStr = RubyString.stringValue(ext).toString();
            if (".*".equals(extStr)) {
                index = name.lastIndexOf('.');
                if (index > 0) {  // -1 no match; 0 it is dot file not extension
                    name = name.substring(0, index);
                }
            } else if (name.endsWith(extStr)) {
                name = name.substring(0, name.length() - extStr.length());
            }
        }

        return RubyString.newString(runtime, RubyString.encodeBytelist(name, origEncoding));
    }

    private static int getSeparatorChar(final RubyClass File) {
        final RubyString sep = RubyString.stringValue(File.getConstant("SEPARATOR"));
        return sep.getByteList().get(0);
    }

    private static int getAltSeparatorChar(final RubyClass File) {
        final IRubyObject sep = File.getConstant("ALT_SEPARATOR");
        if (sep instanceof RubyString) { // != nil
            return ((RubyString) sep).getByteList().get(0);
        }
        return NULL_CHAR;
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

            if (0 != runtime.getPosix().chmod(filename.getAbsolutePath(), (int) mode.getLongValue())) {
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

    static final Pattern PROTOCOL_PATTERN = Pattern.compile(URI_PREFIX_STRING + ".*");

    public static String dirname(ThreadContext context, final String filename) {
        final RubyClass File = context.runtime.getFile();
        IRubyObject sep = File.getConstant("SEPARATOR");
        final String separator; final char separatorChar;
        if (sep instanceof RubyString && ((RubyString) sep).size() == 1) {
            separatorChar = ((RubyString) sep).getByteList().charAt(0);
            separator = (separatorChar == '/') ? "/" : String.valueOf(separatorChar);
        }
        else {
            separator = sep.toString();
            separatorChar = separator.isEmpty() ? '\0' : separator.charAt(0);
        }

        String altSeparator = null; char altSeparatorChar = '\0';
        final IRubyObject rbAltSeparator = File.getConstantNoConstMissing("ALT_SEPARATOR");
        if (rbAltSeparator != null && rbAltSeparator != context.nil) {
            altSeparator = rbAltSeparator.toString();
            if (!altSeparator.isEmpty()) altSeparatorChar = altSeparator.charAt(0);
        }

        String name = filename;
        if (altSeparator != null) name = replace(filename, altSeparator, separator);

        boolean startsWithSeparator = false;
        if (!name.isEmpty()) startsWithSeparator = name.charAt(0) == separatorChar;

        int idx; // jar like paths
        if ((idx = name.indexOf(".jar!")) != -1 && name.startsWith(separator, idx + 5)) {
            int start = name.indexOf("!" + separator) + 1;
            String path = dirname(context, name.substring(start));
            if (path.equals(".") || path.equals(separator)) path = "";
            return name.substring(0, start) + path;
        }
        // address all the url like paths first
        if (PROTOCOL_PATTERN.matcher(name).matches()) {
            int start = name.indexOf(":" + separator) + 2;
            String path = dirname(context, name.substring(start));
            if (path.equals(".")) path = "";
            return name.substring(0, start) + path;
        }

        int minPathLength = 1;
        boolean trimmedSlashes = false;

        boolean startsWithUNCOnWindows = Platform.IS_WINDOWS && startsWith(name, separatorChar, separatorChar);

        if (startsWithUNCOnWindows) minPathLength = 2;

        boolean startsWithDriveLetterOnWindows = startsWithDriveLetterOnWindows(name);

        if (startsWithDriveLetterOnWindows) minPathLength = 3;

        while (name.length() > minPathLength && name.charAt(name.length() - 1) == separatorChar) {
            trimmedSlashes = true;
            name = name.substring(0, name.length() - 1);
        }

        String result;
        if (startsWithDriveLetterOnWindows && name.length() == 2) {
            if (trimmedSlashes) {
                // C:\ is returned unchanged
                result = filename.substring(0, 3);
            } else {
                result = filename.substring(0, 2) + '.';
            }
        } else {
            //TODO deal with UNC names
            int index = name.lastIndexOf(separator);

            if (index == -1) {
                if (startsWithDriveLetterOnWindows) {
                    return filename.substring(0, 2) + '.';
                }
                return ".";
            }
            if (index == 0) {
                return filename.substring(0, 1);
            }

            if (startsWithDriveLetterOnWindows && index == 2) {
                // Include additional path separator
                // (so that dirname of "C:\file.txt" is  "C:\", not "C:")
                index++;
            }

            if (startsWithUNCOnWindows) {
                index = filename.length();
                List<String> split = StringSupport.split(name, separatorChar);
                int pathSectionCount = 0;
                for (int i = 0; i < split.size(); i++) {
                    if (!split.get(i).isEmpty()) pathSectionCount += 1;
                }
                if (pathSectionCount > 2) index = name.lastIndexOf(separator);
            }
            result = filename.substring(0, index);
        }

        // trim leading slashes
        if (startsWithSeparator && result.length() > minPathLength) {
            while ( result.length() > minPathLength &&
                    (result.charAt(minPathLength) == separatorChar ||
                            (altSeparator != null && result.charAt(minPathLength) == altSeparatorChar)) ) {
                result = result.substring(1, result.length());
            }
        }

        char endChar;
        // trim trailing slashes
        while (result.length() > minPathLength) {
            endChar = result.charAt(result.length() - 1);
            if (endChar == separatorChar || (altSeparator != null && endChar == altSeparatorChar)) {
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
        String filename = basename(context, recv, arg).getUnicodeValue();

        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex != (filename.length() - 1)) {
            // Dot is not at beginning and not at end of filename.
            return RubyString.newString(context.runtime, filename.substring(dotIndex));
        }

        return RubyString.newEmptyString(context.runtime);
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
    @JRubyMethod(name = "expand_path", required = 1, optional = 1, meta = true)
    public static IRubyObject expand_path(ThreadContext context, IRubyObject recv, IRubyObject... args) {
        return expandPathInternal(context, args, true, false);
    }

    @Deprecated
    public static IRubyObject expand_path19(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return expand_path(context, recv, args);
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
        return expandPathInternal(context, args, false, false);
    }

    @JRubyMethod(required = 1, optional = 1, meta = true)
    public static IRubyObject realdirpath(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return expandPathInternal(context, args, false, true);
    }

    public static IRubyObject realpath(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        RubyString file = expandPathInternal(context, args, false, true);
        if (!RubyFileTest.exist(context, file)) {
            throw context.runtime.newErrnoENOENTError(file.toString());
        }
        return file;
    }

    @JRubyMethod(meta = true)
    public static IRubyObject realpath(ThreadContext context, IRubyObject recv, IRubyObject path) {
        RubyString file;
        file = StringSupport.checkEmbeddedNulls(context.runtime, get_path(context, path));

        file = expandPathInternal(context, file, null, false, true);
        if (!RubyFileTest.exist(context, file)) {
            throw context.runtime.newErrnoENOENTError(file.toString());
        }
        return file;
    }

    @JRubyMethod(meta = true)
    public static IRubyObject realpath(ThreadContext context, IRubyObject recv, IRubyObject path, IRubyObject cwd) {
        RubyString file;
        file = StringSupport.checkEmbeddedNulls(context.runtime, get_path(context, path));
        RubyString wd = StringSupport.checkEmbeddedNulls(context.runtime, get_path(context, cwd));

        file = expandPathInternal(context, file, wd, false, true);
        if (!RubyFileTest.exist(context, file)) {
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

        ByteList pattern = StringSupport.checkEmbeddedNulls(runtime, args[0].convertToString()).getByteList();
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
        return Dir.fnmatch(pattern.getUnsafeBytes(),
                pattern.getBegin(),
                pattern.getBegin() + pattern.getRealSize(),
                path.getUnsafeBytes(),
                path.getBegin(),
                path.getBegin() + path.getRealSize(),
                flags,
                path.getEncoding()) == 0;
    }

    @JRubyMethod(name = "ftype", required = 1, meta = true)
    public static IRubyObject ftype(ThreadContext context, IRubyObject recv, IRubyObject filename) {
        Ruby runtime = context.runtime;
        RubyString path = StringSupport.checkEmbeddedNulls(runtime, get_path(context, filename));
        return runtime.newFileStat(path.getUnicodeValue(), true).ftype();
    }

    @JRubyMethod(rest = true, meta = true)
    public static RubyString join(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return doJoin(context, recv, args);
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
            if (0 != runtime.getPosix().lchmod(file.toString(), (int) mode.getLongValue())) {
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

        // Try atomic move from JDK
        Path oldPath = Paths.get(oldFile.toURI());
        Path destPath = Paths.get(dest.getAbsolutePath());
        try {
            Files.move(oldPath, destPath, StandardCopyOption.ATOMIC_MOVE);
            return RubyFixnum.zero(runtime);
        } catch (IOException ioe) {
            throw Helpers.newIOErrorFromException(runtime, ioe);
        }
    }

    @JRubyMethod(required = 1, meta = true)
    public static RubyArray split(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        Ruby runtime = context.runtime;
        RubyString filename = StringSupport.checkEmbeddedNulls(runtime, get_path(context, arg));

        return runtime.newArray(dirname(context, recv, filename), basename(context, recv, filename));
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

        if (Platform.IS_WINDOWS) {
            // readlink is not available on Windows. See above and jruby/jruby#3287.
            // TODO: MRI does not implement readlink on Windows, but perhaps we could?
            throw runtime.newNotImplementedError("readlink");
        }

        JRubyFile link = file(path);

        try {
            String realPath = runtime.getPosix().readlink(link.toString());

            if (realPath == null) {
                throw runtime.newErrnoFromLastPOSIXErrno();
            }

            return RubyString.newString(runtime, realPath, runtime.getEncodingService().getFileSystemEncoding());
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
        int oldMask;
        if (args.length == 0) {
            oldMask = PosixShim.umask(runtime.getPosix());
        } else if (args.length == 1) {
            int newMask = (int) args[0].convertToInteger().getLongValue();
            oldMask = PosixShim.umask(runtime.getPosix(), newMask);
        } else {
            throw runtime.newArgumentError("wrong number of arguments");
        }

        return runtime.newFixnum(oldMask);
    }

    @JRubyMethod(required = 2, rest = true, meta = true)
    public static IRubyObject lutime(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        long[] atimeval = null;
        long[] mtimeval = null;

        if (args[0] != context.nil || args[1] != context.nil) {
            atimeval = extractTimespec(context, args[0]);
            mtimeval = extractTimespec(context, args[1]);
        }

        for (int i = 2, j = args.length; i < j; i++) {
            RubyString filename = StringSupport.checkEmbeddedNulls(runtime, get_path(context, args[i]));

            JRubyFile fileToTouch = JRubyFile.create(runtime.getCurrentDirectory(), filename.getUnicodeValue());

            if (!fileToTouch.exists()) {
                throw runtime.newErrnoENOENTError(filename.toString());
            }

            int result = runtime.getPosix().lutimes(fileToTouch.getAbsolutePath(), atimeval, mtimeval);
            if (result == -1) {
                throw runtime.newErrnoFromInt(runtime.getPosix().errno());
            }
        }

        return runtime.newFixnum(args.length - 2);
    }

    @JRubyMethod(required = 2, rest = true, meta = true)
    public static IRubyObject utime(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        long[] atimespec = null;
        long[] mtimespec = null;

        if (args[0] != context.nil || args[1] != context.nil) {
            atimespec = extractTimespec(context, args[0]);
            mtimespec = extractTimespec(context, args[1]);
        }

        for (int i = 2, j = args.length; i < j; i++) {
            RubyString filename = StringSupport.checkEmbeddedNulls(runtime, get_path(context, args[i]));

            JRubyFile fileToTouch = JRubyFile.create(runtime.getCurrentDirectory(),filename.getUnicodeValue());

            if (!fileToTouch.exists()) {
                throw runtime.newErrnoENOENTError(filename.toString());
            }

            int result;

            try {
                result = runtime.getPosix().utimensat(0, fileToTouch.getAbsolutePath(), atimespec, mtimespec, 0);
            } catch (NotImplementedError re) {
                // fall back on utimes
                result = runtime.getPosix().utimes(fileToTouch.getAbsolutePath(), atimespec, mtimespec);
                long[] atimeval = convertTimespecToTimeval(atimespec);
                long[] mtimeval = convertTimespecToTimeval(mtimespec);
                result = runtime.getPosix().utimes(fileToTouch.getAbsolutePath(), atimeval, mtimeval);
            }

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
            JRubyFile file = JRubyFile.create(runtime.getCurrentDirectory(), filename.getUnicodeValue());

            // Broken symlinks considered by exists() as non-existing,
            // so we need to check for symlinks explicitly.
            if (!file.exists() && !isSymlink(context, file)) {
                throw runtime.newErrnoENOENTError(filename.getUnicodeValue());
            }

            if (file.isDirectory() && !isSymlink(context, file)) {
                throw runtime.newErrnoEISDirError(filename.getUnicodeValue());
            }

            if (!file.delete()) {
                throw runtime.newErrnoEACCESError(filename.getUnicodeValue());
            }
        }

        return runtime.newFixnum(args.length);
    }

    private static boolean isSymlink(ThreadContext context, JRubyFile file) {
        return FileResource.wrap(context.runtime.getPosix(), file).isSymLink();
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
        return RubyFixnum.newFixnum(context.runtime, getSize(context));
    }

    final long getSize(ThreadContext context) {
        OpenFile fptr = getOpenFileChecked();

        if ((fptr.getMode() & OpenFile.WRITABLE) != 0) {
            flushRaw(context, false);
        }

        return fptr.posix.size(fptr.fd());
    }

    @JRubyMethod(meta = true)
    public static IRubyObject mkfifo(ThreadContext context, IRubyObject recv, IRubyObject path) {
        if (Platform.IS_WINDOWS) throw context.runtime.newNotImplementedError("mkfifo");

        return mkfifo(context, get_path(context, path), 0666);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject mkfifo(ThreadContext context, IRubyObject recv, IRubyObject path, IRubyObject mode) {
        if (Platform.IS_WINDOWS) throw context.runtime.newNotImplementedError("mkfifo");

        return mkfifo(context, get_path(context, path), RubyNumeric.num2int(mode));
    }

    public static IRubyObject mkfifo(ThreadContext context, RubyString path, int mode) {
        Ruby runtime = context.runtime;
        String decodedPath = JRubyFile.createResource(runtime, path.toString()).absolutePath();

        if (runtime.getPosix().mkfifo(decodedPath, mode) != 0) {
            throw runtime.newErrnoFromInt(runtime.getPosix().errno(), decodedPath);
        }
        return RubyFixnum.zero(runtime);
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
                if (!args[3].isNil()) {
                    options = TypeConverter.convertToTypeWithCheck(context, args[3], context.runtime.getHash(), sites(context).to_hash_checked);
                    if (options.isNil()) {
                        throw runtime.newArgumentError(4, 1, 3);
                    }
                }
                vperm(pm, args[2]);
                vmode(pm, args[1]);
                break;
        }

        int[] oflags_p = {0}, fmode_p = {0};
        IOEncodable convconfig = new ConvConfig();
        EncodingUtils.extractModeEncoding(context, convconfig, pm, options, oflags_p, fmode_p);
        int perm = (vperm(pm) != null && !vperm(pm).isNil()) ?
                RubyNumeric.num2int(vperm(pm)) : 0666;

        return fileOpenGeneric(context, args[0], oflags_p[0], fmode_p[0], convconfig, perm);
    }

    // rb_file_open_generic
    public IRubyObject fileOpenGeneric(ThreadContext context, IRubyObject filename, int oflags, int fmode, IOEncodable convConfig, int perm) {
        Ruby runtime = context.runtime;

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

        fptr.setPath(adjustRootPathOnWindows(runtime, getDecodedPath(context, filename), runtime.getCurrentDirectory()));

        fptr.setFD(sysopen(runtime, fptr.getPath(), oflags, perm));
        fptr.checkTTY();
        if ((fmode_p[0] & OpenFile.SETENC_BY_BOM) != 0) {
            EncodingUtils.ioSetEncodingByBOM(context, this);
        }

        return this;
    }

    public static String getAdjustedPath(ThreadContext context, IRubyObject fileOrPath) {
        return getAdjustedPath(context, fileOrPath, context.runtime.getCurrentDirectory());
    }

    public static String getAdjustedPath(ThreadContext context, IRubyObject fileOrPath, String currentDirectory) {
        Ruby runtime = context.runtime;

        return adjustRootPathOnWindows(runtime, getDecodedPath(context, fileOrPath), currentDirectory);
    }

    // mri: FilePathValue/rb_get_path/rb_get_patch_check
    public static RubyString get_path(ThreadContext context, IRubyObject path) {
        if (path instanceof RubyString) return StringSupport.checkEmbeddedNulls(context.runtime, path);

        FileSites sites = sites(context);
        if (sites.respond_to_to_path.respondsTo(context, path, path, true)) path = sites.to_path.call(context, path, path);

        return filePathConvert(context, path.convertToString());
    }

    // FIXME: MRI skips this logic on windows?  Does not make sense to me why so I left it in.
    // mri: file_path_convert
    private static RubyString filePathConvert(ThreadContext context, RubyString path) {
        StringSupport.checkEmbeddedNulls(context.runtime, path.convertToString());

        if (!Platform.IS_WINDOWS) {
            Ruby runtime = context.getRuntime();
            EncodingService encodingService = runtime.getEncodingService();
            Encoding pathEncoding = path.getEncoding();

            // If we are not ascii and do not match fs encoding then transcode to fs.
            if (runtime.getDefaultInternalEncoding() != null &&
                    pathEncoding != encodingService.getUSAsciiEncoding() &&
                    pathEncoding != encodingService.getAscii8bitEncoding() &&
                    pathEncoding != encodingService.getFileSystemEncoding() &&
                    !path.isAsciiOnly()) {
                path = EncodingUtils.strConvEnc(context, path, pathEncoding, encodingService.getFileSystemEncoding());
            }
        }

        return path;
    }

    /**
     * Get the fully-qualified JRubyFile object for the path, taking into
     * account the runtime's current directory.
     *
     * @param context current thread context
     * @param pathOrFile the string or IO to use for the path
     */
    public static FileResource fileResource(ThreadContext context, IRubyObject pathOrFile) {
        return JRubyFile.createResource(context.runtime, getDecodedPath(context, pathOrFile));
    }

    /**
     * Get the fully-qualified JRubyFile object for the path, taking into
     * account the runtime's current directory.
     *
     * Same as calling {@link #fileResource(ThreadContext, IRubyObject)}
     *
     * @param pathOrFile the string or IO to use for the path
     */
    public static FileResource fileResource(IRubyObject pathOrFile) {
        ThreadContext context = pathOrFile.getRuntime().getCurrentContext();

        return fileResource(context, pathOrFile);
    }

    private static String getDecodedPath(ThreadContext context, IRubyObject pathOrFile) {
        String decodedPath;

        if (pathOrFile instanceof RubyFile) {
            decodedPath = ((RubyFile) pathOrFile).getPath();
        } else if (pathOrFile instanceof RubyIO) {
            decodedPath = ((RubyIO) pathOrFile).openFile.getPath();
        } else {
            RubyString path = get_path(context, pathOrFile);

            if (path.getEncoding() == ASCIIEncoding.INSTANCE) {
                // Best we can do while using JDK file APIs is to hope the path is system default encoding
                ByteList pathBL = path.getByteList();
                decodedPath = new String(pathBL.unsafeBytes(), pathBL.begin(), pathBL.realSize());
            } else {
                // decode as characters
                decodedPath = path.toString();
            }
        }

        return decodedPath;
    }

    @Deprecated // Use fileResource instead
    public static JRubyFile file(IRubyObject pathOrFile) {
        return fileResource(pathOrFile).unwrap(JRubyFile.class);
    }

    @Override
    public String toString() {
        return "RubyFile(" + openFile.getPath() + ", " + openFile.getMode() + ')';
    }

    private static ZipEntry getFileEntry(ZipFile jar, String path, final String prefixForNoEntry) throws IOException {
        ZipEntry entry = jar.getEntry(path);
        if (entry == null) {
            // try canonicalizing the path to eliminate . and .. (JRUBY-4760, JRUBY-4879)
            path = new File(path).getCanonicalPath().substring(prefixForNoEntry.length() + 1);
            entry = jar.getEntry(StringSupport.replaceAll(path, "\\\\", "/").toString());
        }
        return entry;
    }

    @Deprecated // not-used
    public static ZipEntry getDirOrFileEntry(String jar, String path) throws IOException {
        return getDirOrFileEntry(new JarFile(jar), path);
    }

    @Deprecated // not-used
    public static ZipEntry getDirOrFileEntry(ZipFile jar, String path) throws IOException {
        String dirPath = path + '/';
        ZipEntry entry = jar.getEntry(dirPath); // first try as directory
        if (entry == null) {
            if (dirPath.length() == 1) {
                return new ZipEntry(dirPath);
            }
            // try canonicalizing the path to eliminate . and .. (JRUBY-4760, JRUBY-4879)
            final String prefix = new File(".").getCanonicalPath();
            entry = jar.getEntry(new File(dirPath).getCanonicalPath().substring(prefix.length() + 1).replaceAll("\\\\", "/"));

            // JRUBY-6119
            if (entry == null) {
                Enumeration<? extends ZipEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    String zipEntry = entries.nextElement().getName();
                    if (zipEntry.startsWith(dirPath)) {
                        return new ZipEntry(dirPath);
                    }
                }
            }

            if (entry == null) { // try as file
                entry = getFileEntry(jar, path, prefix);
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

    private static boolean isWindowsDriveLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    public static boolean startsWithDriveLetterOnWindows(String path) {
        return startsWithDriveLetterOnWindows((CharSequence) path);
    }

    static boolean startsWithDriveLetterOnWindows(CharSequence path) {
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
        if ((startsWith(path, '/') && !(path.length() > 2 && path.charAt(2) == ':')) || startsWith(path, '\\')) {
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
            path += '/';
        }

        return path;
    }

    /**
     * Extract a timespec (an array of 2 longs: seconds and nanoseconds from epoch) from
     * an IRubyObject.
     */
    private static long[] extractTimespec(ThreadContext context, IRubyObject value) {
        long[] timespec = new long[2];

        if (value instanceof RubyFloat) {
            timespec[0] = Platform.IS_32_BIT ? RubyNumeric.num2int(value) : RubyNumeric.num2long(value);
            double fraction = ((RubyFloat) value).getDoubleValue() % 1.0;
            timespec[1] = (long)(fraction * 1e9 + 0.5);
        } else if (value instanceof RubyNumeric) {
            timespec[0] = Platform.IS_32_BIT ? RubyNumeric.num2int(value) : RubyNumeric.num2long(value);
            timespec[1] = 0;
        } else {
            RubyTime time;
            if (value instanceof RubyTime) {
                time = ((RubyTime) value);
            } else {
                time = (RubyTime) TypeConverter.convertToType(context, value, context.runtime.getTime(), sites(context).to_time_checked, true);
            }
            timespec[0] = Platform.IS_32_BIT ? RubyNumeric.num2int(time.to_i()) : RubyNumeric.num2long(time.to_i());
            timespec[1] = Platform.IS_32_BIT ? RubyNumeric.num2int(time.nsec()) : RubyNumeric.num2long(time.nsec());
        }

        return timespec;
    }

    /**
     * Converts a timespec (array of 2 longs: seconds and nanoseconds from epoch) into a
     * timeval (array of 2 longs: seconds and microseconds from epoch). This is needed because
     * calling methods like utimensat() allows nanosecond precision, while the older utimes()
     * only supports microsecond precision.
     */
    private static long[] convertTimespecToTimeval(long[] timespec) {
        if (timespec == null) {
            return null;
        }

        long[] timeval = new long[2];
        timeval[0] = timespec[0];
        timeval[1] = timespec[1] / 1000;
        return timeval;
    }

    private void checkClosed(ThreadContext context) {
        openFile.checkClosed();
    }

    private static final Pattern PROTOCOL_PREFIX_PATTERN = Pattern.compile(URI_PREFIX_STRING);

    private static RubyString expandPathInternal(ThreadContext context, IRubyObject[] args, boolean expandUser, boolean canonicalize) {
        RubyString path = StringSupport.checkEmbeddedNulls(context.runtime, get_path(context, args[0]));
        RubyString wd = null;
        if (args.length == 2 && args[1] != context.nil) {
            wd = StringSupport.checkEmbeddedNulls(context.runtime, get_path(context, args[1]));
        }
        return expandPathInternal(context, path, wd, expandUser, canonicalize);
    }

    static RubyString expandPathInternal(ThreadContext context, RubyString path, RubyString wd, boolean expandUser, boolean canonicalize) {
        boolean useISO = false;

        ByteList pathByteList = path.getByteList();
        ByteList wdByteList = wd == null ? null : wd.getByteList();

        String relativePath;
        String cwd;

        /*
         In order to support expanding paths marked as binary without breaking any multibyte characters they contain,
         we decode the paths as ISO-8859-1 (raw bytes) here and reencode them below the same way. This allows path
         manipulation to ignore multibyte characters rather than mangling them by incorrectly decoding their bytes.

         Strings not marked as binary are expected to be valid and are decoded using their reported encoding.
        */
        if (pathByteList.getEncoding() == ASCIIEncoding.INSTANCE ||
                (wd != null && wd.getByteList().getEncoding() == ASCIIEncoding.INSTANCE)) {
            // use raw bytes if either are encoded as "binary"
            useISO = true;
            relativePath = RubyEncoding.decodeRaw(pathByteList);
            cwd = wd == null ? null : RubyEncoding.decodeRaw(wdByteList);
        } else {
            // use characters assuming the string is properly encoded
            relativePath = path.toString();
            cwd = wd == null ? null : wd.toString();
        }

        Encoding[] enc = {path.getEncoding()};
        String expanded = expandPath(context, relativePath, enc, cwd, expandUser, canonicalize);

        ByteList expandedByteList;
        if (useISO) {
            // restore the raw bytes and mark them as the new encoding
            expandedByteList = new ByteList(expanded.getBytes(StandardCharsets.ISO_8859_1), enc[0], false);
        } else {
            // encode the characters as the new encoding
            expandedByteList = new ByteList(expanded.getBytes(enc[0].getCharset()), enc[0], false);
        }

        return RubyString.newString(context.runtime, expandedByteList);

    }

    public static String expandPath(ThreadContext context, String relativePath, Encoding[] enc, String cwd, boolean expandUser, boolean canonicalize) {
        Ruby runtime = context.runtime;

        // Encoding logic lives in MRI's rb_file_expand_path_internal and should roughly equate to the following:
        // * Paths expanded from the system, like ~ expanding to the user's home dir, should be filesystem-encoded.
        // * Calls with a relative directory should use the encoding of that string.
        // * Remaining calls should use the encoding of the incoming path string.
        //
        // We have our own system-like cases (e.g. URI expansion) that we do not set to the filesystem encoding
        // because they are not really filesystem paths; they're in-process or network paths.
        //
        // See dac9850 and jruby/jruby#3849.

        // for special paths like ~
        Encoding fsenc = runtime.getEncodingService().getFileSystemEncoding();

        // Special /dev/null of windows
        if (Platform.IS_WINDOWS && ("NUL:".equalsIgnoreCase(relativePath) || "NUL".equalsIgnoreCase(relativePath))) {
            enc[0] = fsenc;
            return concat("//./", relativePath.substring(0, 3));
        }

        // treat uri-like and jar-like path as absolute
        String preFix = "";
        if (relativePath.startsWith("file:")) {
            preFix = "file:";
            relativePath = relativePath.substring(5);
        }
        String postFix = "";
        Matcher protocol = PROTOCOL_PREFIX_PATTERN.matcher(relativePath);
        if (relativePath.contains(".jar!/")) {
            if (protocol.find()) {
                preFix = protocol.group();
                int extra = 0;
                if (relativePath.contains("file://")) {
                    extra = 2;
                    preFix += "//";
                }
                relativePath = relativePath.substring(protocol.end() + extra);
            }
            int index = relativePath.indexOf("!/");
            postFix = relativePath.substring(index);
            relativePath = relativePath.substring(0, index);
        } else if (protocol.find()) {
            preFix = protocol.group();
            int offset = protocol.end();
            String extra = "";
            int index = relativePath.indexOf("file://");
            boolean classloaderURI = preFix.equals("uri:classloader:") || preFix.equals("classpath:");

            if (index >= 0) {
                index += 7; // "file://".length == 7
                // chck if its "file:///"
                if (relativePath.length() > index && relativePath.charAt(index) == '/') {
                    offset += 2; extra = "//";
                } else {
                    offset += 1; extra = "/";
                }
            } else {
                if (classloaderURI && relativePath.startsWith("//", offset)) {
                    // on Windows "uri:classloader://home" ends up as "//home" - trying a network drive!
                    offset += 1; // skip one '/'
                }
            }

            relativePath = relativePath.substring(offset);

            if (classloaderURI) {
                String fakePrefix = Platform.IS_WINDOWS ? "C:/FAKEPATH_PREFIX__" : "/FAKEPATH_PREFIX__";
                relativePath = canonicalizePath(fakePrefix + '/' + relativePath).substring(fakePrefix.length());
            } else {
                relativePath = canonicalizePath(relativePath);
            }

            if (Platform.IS_WINDOWS) {
                // FIXME: If this is only for classLoader uri's then we probably don't need file: check here.
                // Also can we ever get a drive letter in relative path now?
                if (!preFix.contains("file:") && startsWithDriveLetterOnWindows(relativePath)) {
                    // this is basically for classpath:/ and uri:classloader:/
                    relativePath = relativePath.substring(2);
                }
                if (classloaderURI) {
                    relativePath = relativePath.replace('\\', '/');
                }
            }

            return concatStrings(preFix, extra, relativePath);
        }

        String[] uriParts = splitURI(relativePath);

        // Handle ~user paths
        if (expandUser && startsWith(relativePath, '~')) {
            enc[0] = fsenc;
            relativePath = expandUserPath(context, relativePath, true);
        }

        if (uriParts != null) {
            //If the path was an absolute classpath path, return it as-is.
            if (uriParts[0].equals("classpath:")) {
                return concatStrings(preFix, relativePath, postFix);
            }
            relativePath = uriParts[1];
        }

        // Now that we're not treating it as a URI, we need to honor the canonicalize flag.
        // Do not insert early returns below.

        // If there's a second argument, it's the path to which the first argument is relative.
        if (cwd != null) {
            enc[0] = fsenc;
            if (!cwd.startsWith("uri:")) {
                // Handle ~user paths.
                if (expandUser) {
                    cwd = expandUserPath(context, cwd, true);
                }

                // TODO try to treat all uri-like paths alike
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
                // If the path isn't absolute, then prepend the current working directory to the path.
                if (!startsWithSlashNotOnWindows && !startsWithDriveLetterOnWindows(cwd)) {
                    if (cwd.length() == 0) cwd = ".";
                    cwd = JRubyFile.create(runtime.getCurrentDirectory(), cwd).getAbsolutePath();
                }
            }
        } else {
            // If there's no second argument, simply use the working directory of the runtime.
            cwd = runtime.getCurrentDirectory();
        }

        assert cwd != null;

        /*
        The counting of slashes that follows is simply a way to adhere to Ruby's UNC (or something) compatibility.
        When Ruby's expand_path is called with "//foo//bar" it will return "//foo/bar".
        JRuby uses java.io.File, and hence returns "/foo/bar". In order to retain java.io.File in the lower layers
        and provide full Ruby compatibility, the number of extra slashes must be counted and prepended to the result.
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
            } else {
                padSlashes = countSlashes(cwd);
            }
        }

        JRubyFile pathFile;
        if (relativePath.length() == 0) {
            pathFile = JRubyFile.create(relativePath, cwd);
        } else {
            relativePath = adjustRootPathOnWindows(runtime, relativePath, cwd);
            pathFile = JRubyFile.create(cwd, relativePath);
        }

        CharSequence canonicalPath = null;
        if (Platform.IS_WINDOWS && uriParts != null && "classpath:".equals(uriParts[0])) {
            // FIXME: This is all total madness.  we extract off classpath: earlier in processing
            // and then build a absolute path which on windows will stick a drive letter onto it
            // but this is bogus in a classpath file path.  I think the proper fix for expand path
            // is to split out non-file: scheme format paths into a totally different method.  Weaving
            // uri and non-uri paths into one super long method is so rife with hurt that I am literally
            // crying on my keyboard.
            String absolutePath = pathFile.getAbsolutePath();
            if (absolutePath.length() >= 2 && absolutePath.charAt(1) == ':') {
                canonicalPath = canonicalize(null, absolutePath.substring(2));
            }
        }

        if (canonicalPath == null) canonicalPath = canonicalize(null, pathFile.getAbsolutePath());

        CharSequence realPath;
        if (padSlashes.isEmpty()) {
            realPath = canonicalPath;
        }
        else {
            realPath = concat(padSlashes, canonicalPath);
            if (preFix.length() > 0 && padSlashes.startsWith("file:")) {
                realPath = realPath.toString().substring(5);
            }
        }

        if (canonicalize) realPath = canonicalNormalized(realPath);

        if (postFix.contains("..")) postFix = adjustPostFixDotDot(postFix);

        return concatStrings(preFix, realPath, postFix);
    }

    private static String canonicalNormalized(CharSequence realPath) {
        final String path = realPath.toString();
        try {
            return JRubyFile.normalizeSeps(new File(path).getCanonicalPath());
        } catch (IOException ioe) {
            return path;
        }
    }

    private static String adjustPostFixDotDot(String postFix) {
        postFix = '!' + canonicalizePath(postFix.substring(1));
        if (Platform.IS_WINDOWS /* && startsWith(postFix, '!') */) {
            postFix = postFix.replace('\\', '/');
            if (startsWithDriveLetterOnWindows(postFix.substring(1))) {
                postFix = '!' + postFix.substring(3);
            }
        }
        return postFix;
    }

    private static String concatStrings(String s1, CharSequence s2, String s3) {
        return new StringBuilder(s1.length() + s2.length() + s3.length()).append(s1).append(s2).append(s3).toString();
    }

    private static String canonicalizePath(String path) {
        try {
            return new File(path).getCanonicalPath();
        }
        catch (IOException ignore) { return path; }
    }

    public static String[] splitURI(String path) {
        Matcher m = URI_PREFIX.matcher(path);
        if (m.find()) {
            if (m.group(2).length() == 0) {
                return new String[] { path, "" };
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
                return new String[] { path.substring(0, path.indexOf(pathPart)), pathPart };
            } catch (Exception e) {
                try {
                    URL u = new URL(pathWithoutJarPrefix);
                    String pathPart = u.getPath();
                    return new String[] { path.substring(0, path.indexOf(pathPart)), pathPart };
                } catch (MalformedURLException e2) { /* ignore */ }
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
    public static String expandUserPath(ThreadContext context, String path, final boolean raiseOnRelativePath) {
        int pathLength = path.length();

        if (startsWith(path, '~')) {
            // Enebo : Should ~frogger\\foo work (it doesnt in linux ruby)?
            int userEnd = path.indexOf('/');

            if (userEnd == -1) {
                if (pathLength == 1) {
                    // Single '~' as whole path to expand
                    path = RubyDir.getHomeDirectoryPath(context, checkHome(context)).toString();

                    if (raiseOnRelativePath && !isAbsolutePath(path)) {
                        throw context.runtime.newArgumentError("non-absolute home");
                    }
                } else {
                    // No directory delimeter.  Rest of string is username
                    userEnd = pathLength;
                }
            }

            if (userEnd == 1) {
                // '~/...' as path to expand
                path = RubyDir.getHomeDirectoryPath(context, checkHome(context)).toString() + path.substring(1);

                if (raiseOnRelativePath && !isAbsolutePath(path)) {
                    throw context.runtime.newArgumentError("non-absolute home");
                }
            } else if (userEnd > 1){
                // '~user/...' as path to expand
                String user = path.substring(1, userEnd);
                IRubyObject dir = RubyDir.getHomeDirectoryPath(context, user);

                if (dir.isNil()) {
                    throw context.runtime.newArgumentError("user " + user + " does not exist");
                }

                path = dir + (pathLength == userEnd ? "" : path.substring(userEnd));

                // getpwd (or /etc/passwd fallback) returns a home which is not absolute!!! [mecha-unlikely]
                if (raiseOnRelativePath && !isAbsolutePath(path)) {
                    throw context.runtime.newArgumentError("non-absolute home of " + user);
                }
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
    private static String countSlashes(String stringToCheck) {
        // Count number of extra slashes in the beginning of the string.
        int slashCount = 0;

        final int len = stringToCheck.length();
        if (len > 0 && stringToCheck.charAt(0) == '/') {
            // If there are N slashes, then we want N-1.
            if (len > 1 && stringToCheck.charAt(1) == '/') {
                slashCount++;
                for (int i = 2; i < len; i++) {
                    if (stringToCheck.charAt(i) == '/') {
                        slashCount++;
                    } else {
                        break;
                    }
                }
            }
        }

        if (slashCount < SLASHES.length) {
            return SLASHES[slashCount];
        }

        // Prepare a string with the same number of redundant slashes so that we easily can prepend it to the result.
        char[] slashes = new char[slashCount];
        for (int i = 0; i < slashCount; i++) {
            slashes[i] = '/';
        }
        return new String(slashes);
    }

    public static String canonicalize(String path) {
        CharSequence canonical = canonicalize(null, path);
        return canonical == null ? null : canonical.toString();
    }

    private static CharSequence canonicalize(CharSequence canonicalPath, String remaining) {
        if (remaining == null) {
            if (canonicalPath == null) return null;
            if (canonicalPath.length() == 0) return "/";
            // compensate for missing slash after drive letter on windows
            if (startsWithDriveLetterOnWindows(canonicalPath) && canonicalPath.length() == 2) {
                return appendSlash(canonicalPath); // canonicalPath += '/';
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

        CharSequence path = canonicalPath;
        if (child.equals(".")) {
            // no canonical path yet or length is zero, and we have a / followed by a dot...
            if (slash == -1) {
                // we don't have another slash after this, so replace /. with /
                if (canonicalPath != null && canonicalPath.length() == 0) {
                    path = appendSlash(canonicalPath);
                }
            } else {
                // we do have another slash; omit both / and . (JRUBY-1606)
            }
        } else if (child.equals("..")) {
            if (canonicalPath == null) throw new IllegalArgumentException("Cannot have .. at the start of an absolute path");
            String canonicalPathString = canonicalPath.toString();
            int lastDir = canonicalPathString.lastIndexOf('/');
            if (lastDir == -1) {
                if (startsWithDriveLetterOnWindows(canonicalPath)) {
                    // do nothing, we should not delete the drive letter
                } else if (isLocalURI(canonicalPathString)) {
                    // do nothing, leave the URI bits alone
                } else {
                    path = "";
                }
            } else {
                path = canonicalPath.subSequence(0, lastDir);
            }
        } else if (canonicalPath == null) {
            path = child;
        } else {
            // add a slash (if not already there) plus child and recurse) (jruby/jruby#6045)
            int length = canonicalPath.length();
            String canonPathString = canonicalPath.toString();
            if (canonPathString.length() > 0 &&
                    canonicalPath.charAt(length - 1) == '/' &&
                    canonPathString.startsWith("uri:classloader:")) {
                path = canonicalPath + child;
            } else {
                path = canonicalPath + "/" + child;
            }
        }

        return canonicalize(path, remaining);
    }

    private static boolean isLocalURI(String canonicalPathString) {
        return startsWith("classpath:", canonicalPathString) ||
                startsWith("classloader:", canonicalPathString) ||
                startsWith("uri:classloader:", canonicalPathString) ||
                startsWith("file:", canonicalPathString) ||
                startsWith("jar:file", canonicalPathString);
    }

    private static StringBuilder appendSlash(final CharSequence canonicalPath) {
        return new StringBuilder(canonicalPath.length() + 1).append(canonicalPath).append('/');
    }

    /**
     * Check if HOME environment variable is not nil nor empty
     * @param context
     */
    private static RubyString checkHome(ThreadContext context) {
        Ruby runtime = context.runtime;
        IRubyObject home = runtime.getENV().fastARef(RubyString.newStringShared(runtime, RubyDir.HOME));
        if (home == null || home == context.nil || ((RubyString) home).size() == 0) {
            throw runtime.newArgumentError("couldn't find HOME environment -- expanding `~'");
        }
        return (RubyString) home;
    }

    @Override
    public <T> T toJava(Class<T> target) {
        if (target == File.class) {
            final String path = getPath();
            return path == null ? null : (T) new File(path);
        }
        if (target == Path.class || target == Watchable.class) {
            final String path = getPath();
            return path == null ? null : (T) FileSystems.getDefault().getPath(path);
        }
        return super.toJava(target);
    }

    private static RubyString doJoin(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        final Ruby runtime = context.runtime;
        final String separator = runtime.getFile().getConstant("SEPARATOR").toString();

        final RubyArray argsAry = RubyArray.newArrayMayCopy(runtime, args);

        final StringBuilder buffer = new StringBuilder(24);
        boolean isTainted = joinImpl(buffer, separator, context, recv, argsAry);

        RubyString fixedStr = new RubyString(runtime, runtime.getString(), buffer);
        fixedStr.setTaint(isTainted);
        return fixedStr;
    }

    private static boolean joinImpl(final StringBuilder buffer, final String separator,
        ThreadContext context, IRubyObject recv, RubyArray args) {

        boolean isTainted = false;

        for (int i = 0; i < args.size(); i++) {
            final IRubyObject arg = args.eltInternal(i);
            if (arg.isTaint()) isTainted = true;

            final CharSequence element;
            if (arg instanceof RubyString) {
                element = arg.convertToString().getUnicodeValue();
            } else if (arg instanceof RubyArray) {
                if (context.runtime.isInspecting(arg)) {
                    throw context.runtime.newArgumentError("recursive array");
                } else {
                    element = joinImplInspecting(separator, context, recv, args, ((RubyArray) arg));
                }
            } else {
                RubyString path = StringSupport.checkEmbeddedNulls(context.runtime, get_path(context, arg));
                element = path.getUnicodeValue();
            }

            int trailingDelimiterIndex = chomp(buffer);
            boolean trailingDelimiter = trailingDelimiterIndex != -1;
            boolean leadingDelimiter = element.length() > 0 && isDirSeparator(element.charAt(0));
            if (i > 0) {
                if (leadingDelimiter) {
                    // both present delete trailing delimiter(s)
                    if (trailingDelimiter) buffer.delete(trailingDelimiterIndex, buffer.length());
                } else if (!trailingDelimiter) { // no edge delimiters are present add supplied separator
                    buffer.append(separator);
                }
            }
            buffer.append(element);
        }

        return isTainted;
    }

    private static StringBuilder joinImplInspecting(final String separator,
        ThreadContext context, IRubyObject recv, RubyArray parent, RubyArray array) {
        final Ruby runtime = context.runtime;

        final StringBuilder buffer = new StringBuilder(24);
        // If already inspecting, there is no need to register/unregister again.
        if (runtime.isInspecting(parent)) {
            joinImpl(buffer, separator, context, recv, array);
            return buffer;
        }

        try {
            runtime.registerInspecting(parent);
            joinImpl(buffer, separator, context, recv, array);
            return buffer;
        }
        finally {
            runtime.unregisterInspecting(parent);
        }
    }

    // NOTE: MRI and JRuby are both broken here since it does not actually look up
    // File::{SEPARATOR,ALT_SEPARATOR} but merely hardcodes depending on whether we are on Windows.
    private static boolean isDirSeparator(char c) {
        return c == '/' || Platform.IS_WINDOWS && c == '\\';
    }

    // Return the last index before where there is a delimeter.  Otherwise -1.
    // If there are non-consecutive delimeters at the end we will return the
    // first non-delimiter character.
    private static int chomp(final StringBuilder buffer) {
        boolean found = false;

        for (int lastIndex = buffer.length() - 1; lastIndex >= 0; lastIndex--) {
            if (!isDirSeparator(buffer.charAt(lastIndex))) {
                if (found) return lastIndex + 1;
                break;
            }

            found = true;
        }

        return found ? 0 : -1;
    }

    private static String replace(final String str, CharSequence target, CharSequence replace) {
        if (target.length() == 1 && replace.length() == 1) {
            return str.replace(target.charAt(0), replace.charAt(0));
        }
        return str.replace(target, replace);
    }

    private static IRubyObject truncateCommon(ThreadContext context, IRubyObject recv, IRubyObject arg1, IRubyObject arg2) {
        RubyString filename = arg1.convertToString(); // TODO: SafeStringValue here
        Ruby runtime = context.runtime;
        RubyInteger newLength = arg2.convertToInteger();

        File testFile ;
        File childFile = new File(filename.getUnicodeValue());
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

    private static FileSites sites(ThreadContext context) {
        return context.sites.File;
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

    private static final String[] SLASHES = { "", "/", "//" };
    private static final Pattern URI_PREFIX = Pattern.compile("^(jar:)?[a-z]{2,}:(.*)");

    @Deprecated
    protected String path;
}
