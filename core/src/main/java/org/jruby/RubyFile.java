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
import org.jruby.api.API;
import org.jruby.api.Error;
import org.jruby.exceptions.NotImplementedError;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.runtime.*;
import org.jruby.runtime.JavaSites.FileSites;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.encoding.EncodingCapable;
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
import static org.jruby.api.Access.encodingService;
import static org.jruby.api.Access.fileClass;
import static org.jruby.api.Access.hashClass;
import static org.jruby.api.Access.timeClass;
import static org.jruby.api.Check.checkEmbeddedNulls;
import static org.jruby.api.Convert.*;
import static org.jruby.api.Create.*;
import static org.jruby.api.Define.defineClass;
import static org.jruby.api.Error.argumentError;
import static org.jruby.api.Error.runtimeError;
import static org.jruby.runtime.ThreadContext.hasKeywords;
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

    public static RubyClass createFileClass(ThreadContext context, RubyClass IO) {
        // file separator constants
        var separator = newString(context, SLASH).freeze(context);
        var altSeparator = File.separatorChar == '\\' ? newString(context, BACKSLASH).freeze(context) : context.nil;
        var pathSeparator = newString(context, singleCharByteList((byte) File.pathSeparatorChar)).freeze(context);

        RubyClass File = defineClass(context, "File", IO, RubyFile::new).
                reifiedClass(RubyFile.class).
                kindOf(new RubyModule.JavaClassKindOf(RubyFile.class)).
                classIndex(ClassIndex.FILE).
                defineMethods(context, RubyFile.class).
                defineConstant(context, "SEPARATOR", separator).
                defineConstant(context, "Separator", separator).
                defineConstant(context, "ALT_SEPARATOR", altSeparator).
                defineConstant(context, "PATH_SEPARATOR", pathSeparator);

        // For JRUBY-5276, physically define FileTest methods on File's singleton
        File.singletonClass(context).defineMethods(context, RubyFileTest.FileTestFileMethods.class);

        var FileConstants = File.defineModuleUnder(context, "Constants").
                defineConstant(context, "RDONLY", asFixnum(context, OpenFlags.O_RDONLY.intValue())).
                defineConstant(context, "WRONLY", asFixnum(context, OpenFlags.O_WRONLY.intValue())).
                defineConstant(context, "RDWR", asFixnum(context, OpenFlags.O_RDWR.intValue())).
                defineConstant(context, "APPEND", asFixnum(context, OpenFlags.O_APPEND.intValue())).
                defineConstant(context, "CREAT", asFixnum(context, OpenFlags.O_CREAT.intValue())).
                defineConstant(context, "EXCL", asFixnum(context, OpenFlags.O_EXCL.intValue())).
                defineConstant(context, "TRUNC", asFixnum(context, OpenFlags.O_TRUNC.intValue())).
                // FIXME: NOCTTY is showing up as undefined on Linux, but it should be defined.
                defineConstant(context, "NOCTTY", asFixnum(context, OpenFlags.O_NOCTTY.intValue())).
                defineConstant(context, "SHARE_DELETE", asFixnum(context, ModeFlags.SHARE_DELETE)).
                defineConstant(context, "FNM_NOESCAPE", asFixnum(context, FNM_NOESCAPE)).
                defineConstant(context, "FNM_CASEFOLD", asFixnum(context, FNM_CASEFOLD)).
                defineConstant(context, "FNM_SYSCASE", asFixnum(context, FNM_SYSCASE)).
                defineConstant(context, "FNM_DOTMATCH", asFixnum(context, FNM_DOTMATCH)).
                defineConstant(context, "FNM_PATHNAME", asFixnum(context, FNM_PATHNAME)).
                defineConstant(context, "FNM_EXTGLOB", asFixnum(context, FNM_EXTGLOB)).
                defineConstant(context, "LOCK_SH", asFixnum(context, RubyFile.LOCK_SH)).
                defineConstant(context, "LOCK_EX", asFixnum(context, RubyFile.LOCK_EX)).
                defineConstant(context, "LOCK_NB", asFixnum(context, RubyFile.LOCK_NB)).
                defineConstant(context, "LOCK_UN", asFixnum(context, RubyFile.LOCK_UN)).
                defineConstant(context, "NULL", newString(context, getNullDevice()));

        // FIXME: Some comment about an O_DELAY not being defined in OpenFlags.  This might be missing constants.
        // FIXME: Should NONBLOCK exist for Windows fcntl flags?
        if (OpenFlags.O_NONBLOCK.defined()) {
            FileConstants.defineConstant(context, "NONBLOCK", asFixnum(context, OpenFlags.O_NONBLOCK.intValue()));
        } else if (Platform.IS_WINDOWS) {
            FileConstants.defineConstant(context, "NONBLOCK", asFixnum(context, 1));
        }
        
        if (!OpenFlags.O_BINARY.defined()) {
            FileConstants.defineConstant(context, "BINARY", asFixnum(context, 0));
        } else {
            /* disable line code conversion */
            FileConstants.defineConstant(context, "BINARY", asFixnum(context, OpenFlags.O_BINARY.intValue()));
        }

        if (OpenFlags.O_SYNC.defined()) {
            FileConstants.defineConstant(context, "SYNC", asFixnum(context, OpenFlags.O_SYNC.intValue()));
        }
        // O_DSYNC and O_RSYNC are not in OpenFlags
//        #ifdef O_DSYNC
//        /* any write operation perform synchronously except some meta data */
//        constants.setConstant("DSYNC", asFixnum(context, OpenFlags.O_DSYNC.intValue()));
//        #endif
//        #ifdef O_RSYNC
//        /* any read operation perform synchronously. used with SYNC or DSYNC. */
//        constants.setConstant("RSYNC", asFixnum(context, OpenFlags.O_RSYNC.intValue()));
//        #endif
        if (OpenFlags.O_NOFOLLOW.defined()) {
            /* do not follow symlinks */
            FileConstants.defineConstant(context, "NOFOLLOW", asFixnum(context, OpenFlags.O_NOFOLLOW.intValue()));     /* FreeBSD, Linux */
        }
        // O_NOATIME and O_DIRECT are not in OpenFlags
//        #ifdef O_NOATIME
//        /* do not change atime */
//        constants.setConstant("NOATIME", asFixnum(context, OpenFlags.O_NOATIME.intValue()));     /* Linux */
//        #endif
//        #ifdef O_DIRECT
//        /*  Try to minimize cache effects of the I/O to and from this file. */
//        constants.setConstant("DIRECT", asFixnum(context, OpenFlags.O_DIRECT.intValue()));
//        #endif
        if (OpenFlags.O_TMPFILE.defined()) {
            /* Create an unnamed temporary file */
            FileConstants.defineConstant(context, "TMPFILE", asFixnum(context, OpenFlags.O_TMPFILE.intValue()));
        }

        // File::Constants module is included in IO.
        IO.include(context, FileConstants);

        if (Platform.IS_WINDOWS) {
            // readlink is not available on Windows. See below and jruby/jruby#3287.
            // TODO: MRI does not implement readlink on Windows, but perhaps we could?
            File.searchMethod("readlink").setNotImplemented(true);
            File.searchMethod("mkfifo").setNotImplemented(true);
        }

        if (!Platform.IS_BSD) {
            // lchmod appears to be mostly a BSD-ism, not supported on Linux.
            // See https://github.com/jruby/jruby/issues/5547
            File.singletonClass(context).searchMethod("lchmod").setNotImplemented(true);
        }

        return File;
    }

    private static String getNullDevice() {
        // FIXME: MRI defines null devices for Amiga and VMS, but currently we lack ability to detect these platforms
        return Platform.IS_WINDOWS ? "NUL" : "/dev/null";
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

    @JRubyMethod
    public IRubyObject flock(ThreadContext context, IRubyObject operation) {

        // Solaris uses a ruby-ffi version defined in jruby/kernel/file.rb, so re-dispatch
        if (Platform.IS_SOLARIS) return callMethod(context, "flock", operation);

//        int[] op = {0,0};
//        struct timeval time;
//        rb_secure(2);
        int op1 = toInt(context, operation);
        OpenFile fptr = getOpenFileChecked();

        if (fptr.isWritable()) flushRaw(context, false);

        // MRI's logic differs here. For a nonblocking flock that produces EAGAIN, EACCES, or EWOULBLOCK, MRI
        // just returns false immediately. For the same errnos in blocking mode, MRI waits for 0.1s and then
        // attempts the lock again, indefinitely.
        while (fptr.threadFlock(context, op1) < 0) {
            switch (fptr.errno()) {
                case EAGAIN:
                case EACCES:
                case EWOULDBLOCK:
                    if ((op1 & LOCK_NB) != 0) return context.fals;

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
                    throw context.runtime.newErrnoFromErrno(fptr.errno(), fptr.getPath());
            }
        }
        return asFixnum(context, 0);
    }

    // rb_file_initialize
    @JRubyMethod(name = "initialize", required = 1, optional = 3, checkArity = false, visibility = PRIVATE, keywords = true)
    public IRubyObject initialize(ThreadContext context, IRubyObject[] args, Block block) {
        // capture callInfo for delegating to IO#initialize
        int callInfo = context.callInfo;
        IRubyObject keywords = IRRuntimeHelpers.receiveKeywords(context, args, false, true, false);
        // Mild hack. We want to arity-mismatch if extra arg is not really a kwarg but not if it is one.
        int maxArgs = keywords instanceof RubyHash ? 4 : 3;
        int argc = Arity.checkArgumentCount(context, args, 1, maxArgs);

        if (openFile != null) throw runtimeError(context, "reinitializing File");

        if (argc > 0 && argc <= 3) {
            IRubyObject fd = TypeConverter.convertToTypeWithCheck(context, args[0], context.runtime.getFixnum(), sites(context).to_int_checked);
            if (!fd.isNil()) {
                // restore callInfo for delegated call to IO#initialize
                IRRuntimeHelpers.setCallInfo(context, callInfo);
                return switch (argc) {
                    case 1 -> super.initialize(context, fd, block);
                    case 2 -> super.initialize(context, fd, args[1], block);
                    default -> super.initialize(context, fd, args[1], args[2], block);
                };
            }
        }

        return openFile(context, args);
    }

    @JRubyMethod
    public IRubyObject chmod(ThreadContext context, IRubyObject arg) {
        checkClosed(context);
        int mode = toInt(context, arg);
        final String path = getPath();
        if (!new File(path).exists()) throw context.runtime.newErrnoENOENTError(path);

        return asFixnum(context, context.runtime.getPosix().chmod(path, mode));
    }

    @JRubyMethod
    public IRubyObject chown(ThreadContext context, IRubyObject arg1, IRubyObject arg2) {
        checkClosed(context);
        int owner = !arg1.isNil() ? toInt(context, arg1) : -1;
        int group = !arg2.isNil() ? toInt(context, arg2) : -1;

        final String path = getPath();
        if (!new File(path).exists()) throw context.runtime.newErrnoENOENTError(path);

        return asFixnum(context, context.runtime.getPosix().chown(path, owner, group));
    }

    @JRubyMethod
    public IRubyObject atime(ThreadContext context) {
        checkClosed(context);
        return context.runtime.newFileStat(getPath(), false).atime(context);
    }

    @JRubyMethod(name = "ctime")
    public IRubyObject ctime(ThreadContext context) {
        checkClosed(context);
        return context.runtime.newFileStat(getPath(), false).ctime(context);
    }

    @JRubyMethod(name = "birthtime")
    public IRubyObject birthtime(ThreadContext context) {
        checkClosed(context);

        if (!(Platform.IS_WINDOWS || (Platform.IS_BSD && !Platform.IS_OPENBSD))) {
            throw context.runtime.newNotImplementedError("birthtime() function is unimplemented on this machine");
        }

        FileTime btime = getBirthtimeWithNIO(getPath());
        if (btime != null) return context.runtime.newTime(btime.toMillis()); // btime comes in nanos
        return ctime(context);
    }

    public static final FileTime getBirthtimeWithNIO(String pathString) {
        // FIXME: birthtime is in stat, so we should use that if platform supports it (#2152)
        Path path = Paths.get(pathString);
        PosixFileAttributeView view = Files.getFileAttributeView(path, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
        try {
            if (view != null) return view.readAttributes().creationTime();
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
        return ((RubyFileStat) stat(context)).mtime(context);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject path(ThreadContext context, IRubyObject self, IRubyObject str) {
        return get_path(context, str);
    }

    // Moved to IO in 3.2
    public IRubyObject path(ThreadContext context) {
        return super.path(context);
    }

    @JRubyMethod
    public IRubyObject truncate(ThreadContext context, IRubyObject len) {
        long pos = toInt(context, len);
        OpenFile fptr = getOpenFileChecked();
        if (!fptr.isWritable()) throw context.runtime.newIOError("not opened for writing");

        flushRaw(context, false);

        if (pos < 0) throw context.runtime.newErrnoEINVALError(openFile.getPath());

        if (fptr.posix.ftruncate(fptr.fd(), pos) < 0) {
            throw context.runtime.newErrnoFromErrno(fptr.posix.getErrno(), fptr.getPath());
        }

        return asFixnum(context, 0);
    }

    private static final byte[] CLOSED_MESSAGE = new byte[] {' ', '(', 'c', 'l', 'o', 's', 'e', 'd', ')'};

    @JRubyMethod
    public RubyString inspect(ThreadContext context) {
        final String path = openFile.getPath();
        ByteList str = new ByteList((path == null ? 4 : path.length()) + 8);

        str.append('#').append('<');
        str.append(((RubyString) getMetaClass().getRealClass().to_s(context)).getByteList());
        str.append(':').append(path == null ? RubyNil.nilBytes : RubyEncoding.encodeUTF8(path));
        if (!openFile.isOpen()) str.append(CLOSED_MESSAGE);
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

    @Deprecated(since = "9.2.0.0")
    public static IRubyObject basename(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        IRubyObject ext = (args.length > 1 && args[1] != context.nil) ? args[1] : null;
        return basenameImpl(context, (RubyClass) recv, args[0], ext);
    }

    private static RubyString basenameImpl(ThreadContext context, RubyClass klass, IRubyObject path, IRubyObject ext) {
        final int separatorChar = getSeparatorChar(context, klass);
        final int altSeparatorChar = getAltSeparatorChar(context, klass);

        RubyString origString = get_path(context, path);
        Encoding origEncoding = origString.getEncoding();
        String name = origString.toString();

        // uri-like paths without parent directory
        if (name.endsWith(".jar!/") || ROOT_PATTERN.matcher(name).matches()) return (RubyString) path;

        // MRI-compatible basename handling for windows drive letter paths
        if (Platform.IS_WINDOWS) {
            if (name.length() > 1 && name.charAt(1) == ':' && Character.isLetter(name.charAt(0))) {
                switch (name.length()) {
                case 2:
                    return RubyString.newEmptyString(context.runtime, origString.getEncoding());
                case 3:
                    return newString(context, RubyString.encodeBytelist(name.substring(2), origEncoding));
                default:
                    switch (name.charAt(2)) {
                    case '/', '\\':
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

        return newString(context, RubyString.encodeBytelist(name, origEncoding));
    }

    private static int getSeparatorChar(ThreadContext context, final RubyClass File) {
        final RubyString sep = RubyString.stringValue(File.getConstant(context, "SEPARATOR"));
        return sep.getByteList().get(0);
    }

    private static int getAltSeparatorChar(ThreadContext context, final RubyClass File) {
        final IRubyObject sep = File.getConstant(context, "ALT_SEPARATOR");
        return sep instanceof RubyString sepStr ? sepStr.getByteList().get(0) : NULL_CHAR;
    }

    @JRubyMethod(required = 2, rest = true, checkArity = false, meta = true)
    public static IRubyObject chmod(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        int argc = Arity.checkArgumentCount(context, args, 2, -1);
        int count = 0;
        int mode = toInt(context, args[0]);

        for (int i = 1; i < argc; i++, count++) {
            JRubyFile filename = file(args[i]);

            if (!filename.exists()) throw context.runtime.newErrnoENOENTError(filename.toString());

            if (0 != context.runtime.getPosix().chmod(filename.getAbsolutePath(), mode)) {
                throw context.runtime.newErrnoFromLastPOSIXErrno();
            }
        }

        return asFixnum(context, count);
    }

    @JRubyMethod(required = 2, rest = true, checkArity = false, meta = true)
    public static IRubyObject chown(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        int argc = Arity.checkArgumentCount(context, args, 2, -1);
        int count = 0;
        int owner = !args[0].isNil() ? toInt(context, args[0]) : -1;
        int group = !args[1].isNil() ? toInt(context, args[1]) : -1;

        for (int i = 2; i < argc; i++) {
            JRubyFile filename = file(args[i]);

            if (!filename.exists()) throw context.runtime.newErrnoENOENTError(filename.toString());

            if (0 != context.runtime.getPosix().chown(filename.getAbsolutePath(), owner, group)) {
                throw context.runtime.newErrnoFromLastPOSIXErrno();
            } else {
                count++;
            }
        }

        return asFixnum(context, count);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject dirname(ThreadContext context, IRubyObject recv, IRubyObject path) {
        return dirnameCommon(context, get_path(context, path), 1);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject dirname(ThreadContext context, IRubyObject recv, IRubyObject path, IRubyObject arg1) {
        return dirnameCommon(context, get_path(context, path), toInt(context, arg1));
    }

    private static RubyString dirnameCommon(ThreadContext context, RubyString filename, int level) {
        return newString(context, dirname(context, filename.asJavaString(), level));
    }

    static final Pattern PROTOCOL_PATTERN = Pattern.compile(URI_PREFIX_STRING + ".*");

    public static String dirname(ThreadContext context, final String filename) {
        return dirname(context, filename, 1);
    }

    public static String dirname(ThreadContext context, final String filename, int level) {
        if (level < 0) throw argumentError(context, "negative level: " + level);

        final RubyClass File = fileClass(context);
        IRubyObject sep = File.getConstant(context, "SEPARATOR");
        final String separator; final char separatorChar;
        if (sep instanceof RubyString && ((RubyString) sep).size() == 1) {
            separatorChar = ((RubyString) sep).getByteList().charAt(0);
            separator = (separatorChar == '/') ? "/" : String.valueOf(separatorChar);
        } else {
            separator = sep.toString();
            separatorChar = separator.isEmpty() ? '\0' : separator.charAt(0);
        }

        String altSeparator = null; char altSeparatorChar = '\0';
        final IRubyObject rbAltSeparator = File.getConstantNoConstMissing(context, "ALT_SEPARATOR");
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
                return level == 0 ? name : ".";
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
            for (int i = level - 1; i > 0; i--) {
                int _index = result.lastIndexOf(separator);
                if (_index == 0) {
                    result = separator;
                    break;
                } else if (_index != -1) {
                    result = result.substring(0, _index);
                }
            }
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
    @JRubyMethod(meta = true)
    public static IRubyObject extname(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        String filename = basename(context, recv, arg).toString();
        int dotIndex = filename.indexOf('.');

        if (dotIndex < 0) return newEmptyString(context);

        int length = filename.length();
        if (dotIndex == length - 1 && length != 1) return newString(context, filename.substring(dotIndex));

        int e = dotIndex;
        // skip through whole sequence of '........'
        for (e = e + 1; e < length; e++) {
            if (filename.charAt(e) != '.') {
                e = e - 1;
                break;
            }
        }

        if (e == length) return newEmptyString(context);  // all dots

        for (int i = e; i < length; i++) {
            char c = filename.charAt(i);
            if (c == '.' || c == ' ') e = i;
        }

        return e == 0 ? newEmptyString(context) : newString(context, filename.substring(e));
    }

    /**
     * Converts a pathname to an absolute pathname. Relative paths are
     * referenced from the current working directory of the process.
     * @param recv
     * @param path
     * @return Resulting absolute path as a String
     */
    @JRubyMethod(name = "expand_path", meta = true)
    public static IRubyObject expand_path(ThreadContext context, IRubyObject recv, IRubyObject path) {
        return expandPathInternal(context, path, true, false);
    }

    /**
     * Converts a pathname to an absolute pathname. Relative paths are
     * referenced from the given working directory. If the second argument is also relative, it will
     * first be converted to an absolute pathname.
     * @param recv
     * @param path
     * @param wd
     * @return Resulting absolute path as a String
     */
    @JRubyMethod(name = "expand_path", meta = true)
    public static IRubyObject expand_path(ThreadContext context, IRubyObject recv, IRubyObject path, IRubyObject wd) {
        return expandPathInternal(context, path, wd, true, false);
    }

    /**
     *      Converts a pathname to an absolute pathname. Relative paths are
     *      referenced from the current working directory of the process.
     *      If the given pathname starts with a ``+~+'' it is
     *      NOT expanded, it is treated as a normal directory name.
     *
     * @param context
     * @param recv
     * @param path
     * @return
     */
    @JRubyMethod(meta = true)
    public static IRubyObject absolute_path(ThreadContext context, IRubyObject recv, IRubyObject path) {
        return expandPathInternal(context, path, false, false);
    }

    /**
     *      Converts a pathname to an absolute pathname. Relative paths are
     *      referenced from _dir_. If the given pathname starts with a ``+~+'' it is
     *      NOT expanded, it is treated as a normal directory name.
     *
     * @param context
     * @param recv
     * @param path
     * @param dir
     * @return
     */
    @JRubyMethod(meta = true)
    public static IRubyObject absolute_path(ThreadContext context, IRubyObject recv, IRubyObject path, IRubyObject dir) {
        return expandPathInternal(context, path, dir, false, false);
    }

    @JRubyMethod(name = "absolute_path?", meta = true)
    public static IRubyObject absolute_path_p(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        RubyString file = get_path(context, arg);

        // asJavaString should be ok here since windows drive shares will be charset representable and otherwise we look for "/" at front.
        return asBoolean(context, isJRubyAbsolutePath(file.asJavaString()));
    }

    @JRubyMethod(meta = true)
    public static IRubyObject realdirpath(ThreadContext context, IRubyObject recv, IRubyObject path) {
        return expandPathInternal(context, path, false, true);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject realdirpath(ThreadContext context, IRubyObject recv, IRubyObject path, IRubyObject dir) {
        return expandPathInternal(context, path, dir, false, true);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject realpath(ThreadContext context, IRubyObject recv, IRubyObject path) {
        return realPathCommon(context, get_path(context, path), null);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject realpath(ThreadContext context, IRubyObject recv, IRubyObject path, IRubyObject cwd) {
        return realPathCommon(context, get_path(context, path), get_path(context, cwd));
    }

    private static RubyString realPathCommon(ThreadContext context, RubyString file, RubyString cwd) {
        file = expandPathInternal(context, file, cwd, false, true);
        if (!RubyFileTest.exist(context, file)) throw context.runtime.newErrnoENOENTError(file.toString());
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
    @JRubyMethod(name = {"fnmatch", "fnmatch?"}, meta = true)
    public static IRubyObject fnmatch(ThreadContext context, IRubyObject recv, IRubyObject _pattern, IRubyObject _path) {
        return fnmatchCommon(context, _pattern, _path, 0);
    }

    @JRubyMethod(name = {"fnmatch", "fnmatch?"}, meta = true)
    public static IRubyObject fnmatch(ThreadContext context, IRubyObject recv, IRubyObject _pattern, IRubyObject _path, IRubyObject _flags) {
        return fnmatchCommon(context, _pattern, _path, toInt(context, _flags));
    }

    private static RubyBoolean fnmatchCommon(ThreadContext context, IRubyObject _path, IRubyObject _pattern, int flags) {
        boolean braces_match = false;
        boolean extglob = (flags & FNM_EXTGLOB) != 0;
        ByteList pattern = checkEmbeddedNulls(context, _path.convertToString()).getByteList();
        ByteList path = checkEmbeddedNulls(context, get_path(context, _pattern)).getByteList();

        if (extglob) {
            String spattern = _path.asJavaString();
            ArrayList<String> patterns = Dir.braces(spattern, flags, new ArrayList<>());

            boolean matches = false;
            for(int i = 0; i < patterns.size(); i++) {
                matches |= dir_fnmatch(new ByteList(patterns.get(i).getBytes()), path, flags);
            }
            braces_match = matches;
        }

        return braces_match || dir_fnmatch(pattern, path, flags) ? context.tru : context.fals;
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

    @JRubyMethod(name = "ftype", meta = true)
    public static IRubyObject ftype(ThreadContext context, IRubyObject recv, IRubyObject filename) {
        return context.runtime.newFileStat(get_path(context, filename).toString(), true).ftype(context);
    }

    @JRubyMethod(rest = true, meta = true)
    public static RubyString join(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return doJoin(context, recv, args);
    }

    @JRubyMethod(name = "lstat", meta = true)
    public static IRubyObject lstat(ThreadContext context, IRubyObject recv, IRubyObject filename) {
        return context.runtime.newFileStat(get_path(context, filename).toString(), true);
    }

    @JRubyMethod(name = "stat", meta = true)
    public static IRubyObject stat(ThreadContext context, IRubyObject recv, IRubyObject filename) {
        return context.runtime.newFileStat(get_path(context, filename).toString(), false);
    }

    @JRubyMethod(name = "atime", meta = true)
    public static IRubyObject atime(ThreadContext context, IRubyObject recv, IRubyObject filename) {
        return context.runtime.newFileStat(get_path(context, filename).toString(), false).atime(context);
    }

    @JRubyMethod(name = "ctime", meta = true)
    public static IRubyObject ctime(ThreadContext context, IRubyObject recv, IRubyObject filename) {
        return context.runtime.newFileStat(get_path(context, filename).toString(), false).ctime(context);
    }

    @JRubyMethod(name = "birthtime", meta = true)
    public static IRubyObject birthtime(ThreadContext context, IRubyObject recv, IRubyObject filename) {
        return context.runtime.newFileStat(get_path(context, filename).toString(), false).birthtime(context);
    }

    @JRubyMethod(required = 1, rest = true, checkArity = false, meta = true)
    public static IRubyObject lchmod(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        int argc = Arity.checkArgumentCount(context, args, 1, -1);
        int count = 0;
        int mode = toInt(context, args[0]);

        for (int i = 1; i < argc; i++, count++) {
            JRubyFile file = file(args[i]);
            if (context.runtime.getPosix().lchmod(file.toString(), mode) != 0) {
                throw context.runtime.newErrnoFromLastPOSIXErrno();
            }
        }

        return asFixnum(context, count);
    }

    @JRubyMethod(required = 2, rest = true, checkArity = false, meta = true)
    public static IRubyObject lchown(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        int argc = Arity.checkArgumentCount(context, args, 2, -1);
        int owner = !args[0].isNil() ? toInt(context, args[0]) : -1;
        int group = !args[1].isNil() ? toInt(context, args[1]) : -1;
        int count = 0;

        for (int i = 2; i < argc; i++) {
            JRubyFile file = file(args[i]);

            if (0 != context.runtime.getPosix().lchown(file.toString(), owner, group)) {
                throw context.runtime.newErrnoFromLastPOSIXErrno();
            } else {
                count++;
            }
        }

        return asFixnum(context, count);
    }

    @JRubyMethod(meta = true)
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
        return asFixnum(context, ret);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject mtime(ThreadContext context, IRubyObject recv, IRubyObject filename) {
        return context.runtime.newFileStat(get_path(context, filename).toString(), false).mtime(context);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject rename(ThreadContext context, IRubyObject recv, IRubyObject oldName, IRubyObject newName) {
        RubyString oldNameString = get_path(context, oldName);
        RubyString newNameString = get_path(context, newName);
        String newNameJavaString = newNameString.toString();
        String oldNameJavaString = oldNameString.toString();

        var cwd = context.runtime.getCurrentDirectory();
        JRubyFile oldFile = JRubyFile.create(cwd, oldNameJavaString);
        JRubyFile newFile = JRubyFile.create(cwd, newNameJavaString);

        boolean isOldSymlink = RubyFileTest.symlink_p(context, recv, oldNameString).isTrue();
        // Broken symlinks considered by exists() as non-existing,
        // so we need to check for symlinks explicitly.
        if (!(oldFile.exists() || isOldSymlink) || !newFile.getParentFile().exists()) {
            throw context.runtime.newErrnoENOENTError(oldNameJavaString + " or " + newNameJavaString);
        }

        JRubyFile dest = JRubyFile.create(cwd, newNameJavaString);
        if (oldFile.renameTo(dest)) return asFixnum(context, 0);           // rename is successful

        // rename via Java API call wasn't successful, let's try some tricks, similar to MRI

        if (newFile.exists()) {
            context.runtime.getPosix().chmod(newNameJavaString, 0666);
            newFile.delete();
        }

        // Try atomic move from JDK
        Path oldPath = Paths.get(oldFile.toURI());
        Path destPath = Paths.get(dest.getAbsolutePath());
        try {
            Files.move(oldPath, destPath, StandardCopyOption.ATOMIC_MOVE);
            return asFixnum(context, 0);
        } catch (IOException ioe) {
            throw Helpers.newIOErrorFromException(context.runtime, ioe);
        }
    }

    @JRubyMethod(meta = true)
    public static RubyArray split(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        RubyString filename = get_path(context, arg);

        return newArray(context, dirname(context, recv, filename), basename(context, recv, filename));
    }

    @JRubyMethod(meta = true)
    public static IRubyObject symlink(ThreadContext context, IRubyObject recv, IRubyObject from, IRubyObject to) {
        Ruby runtime = context.runtime;

        RubyString fromStr = get_path(context, from);
        RubyString toStr = get_path(context, to);
        String tovalue = toStr.toString();
        tovalue = JRubyFile.create(runtime.getCurrentDirectory(), tovalue).getAbsolutePath();
        try {
            if (runtime.getPosix().symlink(fromStr.toString(), tovalue) == -1) {
                if (runtime.getPosix().isNative()) {
                    throw runtime.newErrnoFromInt(runtime.getPosix().errno(), String.format("(%s, %s)", fromStr, toStr));
                } else {
                    throw runtime.newErrnoEEXISTError(String.format("(%s, %s)", fromStr, toStr));
                }
            }
        } catch (java.lang.UnsatisfiedLinkError ule) {
            throw runtime.newNotImplementedError("symlink() function is unimplemented on this machine");
        }

        return asFixnum(context, 0);
    }

    @JRubyMethod(meta = true)
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

            return RubyString.newString(runtime, realPath, encodingService(context).getFileSystemEncoding());
        } catch (IOException e) {
            throw runtime.newIOError(e.getMessage());
        }
    }

    @Deprecated(since = "10.0.0.0")
    public static IRubyObject truncate19(ThreadContext context, IRubyObject recv, IRubyObject path, IRubyObject length) {
        return truncate(context, recv, path, length);
    }

    // Can we produce IOError which bypasses a close?
    @JRubyMethod(name = "truncate", meta = true)
    public static IRubyObject truncate(ThreadContext context, IRubyObject recv, IRubyObject path, IRubyObject length) {
        return truncateCommon(context, recv, get_path(context, path), length);
    }

    @JRubyMethod(meta = true, optional = 1, checkArity = false)
    public static IRubyObject umask(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        int argc = Arity.checkArgumentCount(context, args, 0, 1);
        int oldMask = argc == 0 ?
                PosixShim.umask(context.runtime.getPosix()) :
                PosixShim.umask(context.runtime.getPosix(), toInt(context, args[0]));

        return asFixnum(context, oldMask);
    }

    @JRubyMethod(required = 2, rest = true, checkArity = false, meta = true)
    public static IRubyObject lutime(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        int argc = Arity.checkArgumentCount(context, args, 2, -1);

        Ruby runtime = context.runtime;
        long[] atimeval = null;
        long[] mtimeval = null;

        if (args[0] != context.nil || args[1] != context.nil) {
            atimeval = convertTimespecToTimeval(extractTimespec(context, args[0]));
            mtimeval = convertTimespecToTimeval(extractTimespec(context, args[1]));
        }

        for (int i = 2, j = argc; i < j; i++) {
            var filename = get_path(context, args[i]).toString();
            JRubyFile fileToTouch = JRubyFile.create(runtime.getCurrentDirectory(), filename);
            if (!fileToTouch.exists()) throw runtime.newErrnoENOENTError(filename);

            int result = runtime.getPosix().lutimes(fileToTouch.getAbsolutePath(), atimeval, mtimeval);
            if (result == -1) throw runtime.newErrnoFromInt(runtime.getPosix().errno());
        }

        return asFixnum(context, argc - 2);
    }

    @JRubyMethod(required = 2, rest = true, checkArity = false, meta = true)
    public static IRubyObject utime(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        int argc = Arity.checkArgumentCount(context, args, 2, -1);

        Ruby runtime = context.runtime;
        long[] atimespec = null;
        long[] mtimespec = null;

        if (args[0] != context.nil || args[1] != context.nil) {
            atimespec = extractTimespec(context, args[0]);
            mtimespec = extractTimespec(context, args[1]);
        }

        for (int i = 2, j = argc; i < j; i++) {
            var filename = get_path(context, args[i]).toString();

            JRubyFile fileToTouch = JRubyFile.create(runtime.getCurrentDirectory(), filename);
            if (!fileToTouch.exists()) throw runtime.newErrnoENOENTError(filename);

            int result;
            try {
                result = runtime.getPosix().utimensat(0, fileToTouch.getAbsolutePath(), atimespec, mtimespec, 0);
            } catch (NotImplementedError re) {
                // fall back on utimes
                result = runtime.getPosix().utimes(fileToTouch.getAbsolutePath(),
                        convertTimespecToTimeval(atimespec), convertTimespecToTimeval(mtimespec));
            }

            if (result == -1) throw runtime.newErrnoFromInt(runtime.getPosix().errno());
        }

        return asFixnum(context, argc - 2);
    }

    @JRubyMethod(rest = true, meta = true)
    public static IRubyObject delete(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        for (int i = 0; i < args.length; i++) {
            var filename = get_path(context, args[i]).toString();
            JRubyFile file = JRubyFile.create(context.runtime.getCurrentDirectory(), filename);

            // Broken symlinks considered by exists() as non-existing,
            // so we need to check for symlinks explicitly.
            if (!file.exists() && !isSymlink(context, file)) throw context.runtime.newErrnoENOENTError(filename);
            if (file.isDirectory() && !isSymlink(context, file)) throw context.runtime.newErrnoEISDirError(filename);
            if (!file.delete()) throw context.runtime.newErrnoEACCESError(filename);
        }

        return asFixnum(context, args.length);
    }

    private static boolean isSymlink(ThreadContext context, JRubyFile file) {
        return FileResource.wrap(context.runtime.getPosix(), file).isSymLink();
    }

    @JRubyMethod(rest = true, meta = true)
    public static IRubyObject unlink(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        POSIX posix = context.runtime.getPosix();

        if (!posix.isNative() || Platform.IS_WINDOWS) return delete(context, recv, args);

        var cwd = context.runtime.getCurrentDirectory();
        for (int i = 0; i < args.length; i++) {
            var toUnlink = JRubyFile.create(cwd, get_path(context, args[i]).toString()).getAbsolutePath();

            if (posix.unlink(toUnlink) < 0) throw context.runtime.newErrnoFromInt(posix.errno());
        }

        return asFixnum(context, args.length);
    }

    public static IRubyObject unlink(ThreadContext context, IRubyObject... args) {
        return unlink(context, fileClass(context), args);
    }

    // rb_file_size but not using stat
    @JRubyMethod
    public IRubyObject size(ThreadContext context) {
        return asFixnum(context, getSize(context));
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

        return mkfifo(context, get_path(context, path), toInt(context, mode));
    }

    public static IRubyObject mkfifo(ThreadContext context, RubyString path, int mode) {
        Ruby runtime = context.runtime;
        String decodedPath = JRubyFile.createResource(runtime, path.toString()).absolutePath();

        if (runtime.getPosix().mkfifo(decodedPath, mode) != 0) {
            throw runtime.newErrnoFromInt(runtime.getPosix().errno(), decodedPath);
        }
        return RubyFixnum.zero(runtime);
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
        API.ModeAndPermission pm = new API.ModeAndPermission(null, null);
        IRubyObject options = context.nil;

        switch(args.length) {
            case 1:
                break;
            case 2: {
                IRubyObject test = TypeConverter.checkHashType(context.runtime, args[1]);
                if (test instanceof RubyHash) {
                    options = test;
                } else {
                    vmode(pm, args[1]);
                }
                break;
            }
            case 3: {
                IRubyObject test = TypeConverter.checkHashType(context.runtime, args[2]);
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
                    options = TypeConverter.convertToTypeWithCheck(context, args[3], hashClass(context), sites(context).to_hash_checked);
                    if (options.isNil()) throw argumentError(context, 4, 1, 3);
                }
                vperm(pm, args[2]);
                vmode(pm, args[1]);
                break;
        }

        int[] oflags_p = {0}, fmode_p = {0};
        IOEncodable convconfig = new ConvConfig();
        EncodingUtils.extractModeEncoding(context, convconfig, pm, options, oflags_p, fmode_p);
        int perm = (vperm(pm) != null && !vperm(pm).isNil()) ?
                toInt(context, vperm(pm)) : 0666;

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
        return getPathCheckConvert(context, getPathCheckToString(context, path));
    }

    // CRuby rb_get_path_check_convert
    private static RubyString getPathCheckConvert(ThreadContext context, RubyString path) {
        path = filePathConvert(context, path);

        checkPathEncoding(context, path);

        checkEmbeddedNulls(context, path);

        return path.strDup(context.runtime);
    }

    // CRuby: check_path_encoding
    private static Encoding checkPathEncoding(ThreadContext context, RubyString str) {
        Encoding enc = str.getEncoding();
        if (!enc.isAsciiCompatible()) {
            throw Error.encodingCompatibilityError(context, "path name must be ASCII-compatible (" + enc + "): " + str);
        }
        return enc;
    }

    // CRuby: rb_get_path_check_to_string
    private static RubyString getPathCheckToString(ThreadContext context, IRubyObject path) {
        if (path instanceof RubyString) return (RubyString) path;

        FileSites sites = sites(context);
        if (sites.respond_to_to_path.respondsTo(context, path, path, true)) path = sites.to_path.call(context, path, path);

        return path.convertToString();
    }

    // FIXME: MRI skips this logic on windows?  Does not make sense to me why so I left it in.
    // mri: file_path_convert
    protected static RubyString filePathConvert(ThreadContext context, RubyString path) {
        checkEmbeddedNulls(context, path);

        if (!Platform.IS_WINDOWS) {
            var encodingService = encodingService(context);
            Encoding pathEncoding = path.getEncoding();

            // If we are not ascii and do not match fs encoding then transcode to fs.
            if (context.runtime.getDefaultInternalEncoding() != null &&
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
    @Deprecated(since = "9.4-") // since 2020
    public static FileResource fileResource(IRubyObject pathOrFile) {
        return fileResource(((RubyBasicObject)pathOrFile).getCurrentContext(), pathOrFile);
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

    @Deprecated(since = "1.7.11") // Use fileResource instead
    public static JRubyFile file(IRubyObject pathOrFile) {
        return fileResource(((RubyBasicObject) pathOrFile).getCurrentContext(), pathOrFile).unwrap(JRubyFile.class);
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

    @Deprecated(since = "9.1.11.0") // not-used
    public static ZipEntry getDirOrFileEntry(String jar, String path) throws IOException {
        return getDirOrFileEntry(new JarFile(jar), path);
    }

    @Deprecated(since = "9.2.0.0") // not-used
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

    private static boolean isJRubyAbsolutePath(String path) {
        return isAbsolutePath(path) ||
                path.startsWith("classpath:/") ||
                path.startsWith("classpath:uri:/") ||
                path.startsWith("uri:/") ||
                path.startsWith("uri:classloader:/") ||
                path.startsWith("uri:file:/") ||
                path.startsWith("file:/") ||
                path.contains(".jar!/");
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

        if (value instanceof RubyFloat flote) {
            timespec[0] = Platform.IS_32_BIT ? flote.asInt(context) : flote.asLong(context);
            double fraction = flote.asDouble(context) % 1.0;
            timespec[1] = (long)(fraction * 1e9 + 0.5);
        } else if (value instanceof RubyNumeric numeric) {
            timespec[0] = Platform.IS_32_BIT ? numeric.asInt(context) : numeric.asLong(context);
            timespec[1] = 0;
        } else {
            RubyTime time = value instanceof RubyTime t ?
                    t : (RubyTime) TypeConverter.convertToType(context, value, timeClass(context), sites(context).to_time_checked, true);

            timespec[0] = Platform.IS_32_BIT ? time.to_i(context).asInt(context) : time.to_i(context).asLong(context);
            timespec[1] = Platform.IS_32_BIT ? time.nsec(context).asInt(context) : time.nsec(context).asLong(context);
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

    private static RubyString expandPathInternal(ThreadContext context, IRubyObject arg0, boolean expandUser, boolean canonicalize) {
        return expandPathInternal(context, arg0, null, expandUser, canonicalize);
    }

    private static RubyString expandPathInternal(ThreadContext context, IRubyObject _path, IRubyObject _wd, boolean expandUser, boolean canonicalize) {
        RubyString path = get_path(context, _path);
        RubyString wd = null;
        if (_wd != null && _wd != context.nil) {
            if (_wd instanceof RubyHash) {
                // FIXME : do nothing when arg1 is Hash(e.g. {:mode=>0}, {:encoding=>"ascii-8bit"})
            } else {
                wd = get_path(context, _wd);
            }
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

        return newString(context, expandedByteList);

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
        Encoding fsenc = encodingService(context).getFileSystemEncoding();

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
                        && !Platform.IS_WINDOWS && !cwd.isEmpty()
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
            if (!relativePath.isEmpty() && relativePath.charAt(0) == '/') {
                padSlashes = countSlashes(relativePath);
            } else {
                padSlashes = countSlashes(cwd);
            }
        }

        JRubyFile pathFile;
        if (relativePath.isEmpty()) {
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
                        throw argumentError(context, "non-absolute home");
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
                    throw argumentError(context, "non-absolute home");
                }
            } else if (userEnd > 1){
                // '~user/...' as path to expand
                String user = path.substring(1, userEnd);
                IRubyObject dir = RubyDir.getHomeDirectoryPath(context, user);

                if (dir.isNil()) {
                    throw argumentError(context, "user " + user + " does not exist");
                }

                path = dir + (pathLength == userEnd ? "" : path.substring(userEnd));

                // getpwd (or /etc/passwd fallback) returns a home which is not absolute!!! [mecha-unlikely]
                if (raiseOnRelativePath && !isAbsolutePath(path)) {
                    throw argumentError(context, "non-absolute home of " + user);
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
        IRubyObject home = context.runtime.getENV().fastARef(newSharedString(context, RubyDir.HOME));
        if (home == null || home == context.nil || ((RubyString) home).size() == 0) {
            throw argumentError(context, "couldn't find HOME environment -- expanding '~'");
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
        final String separator = fileClass(context).getConstant(context, "SEPARATOR").toString();
        final RubyArray argsAry = RubyArray.newArrayMayCopy(context.runtime, args);
        final StringBuilder buffer = new StringBuilder(24);

        joinImpl(buffer, separator, context, recv, argsAry);

        return newString(context, buffer.toString());
    }

    private static boolean joinImpl(final StringBuilder buffer, final String separator,
        ThreadContext context, IRubyObject recv, RubyArray args) {

        for (int i = 0; i < args.size(); i++) {
            final IRubyObject arg = args.eltInternal(i);

            final String element;
            if (arg instanceof RubyString) {
                element = arg.convertToString().toString();
            } else if (arg instanceof RubyArray ary) {
                if (context.runtime.isInspecting(arg)) throw argumentError(context, "recursive array");

                element = joinImplInspecting(separator, context, recv, args, ary).toString();
            } else {
                element = get_path(context, arg).toString();
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

        return false; // used to be tainted value when tainting existed.
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
        } finally {
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
        RubyInteger newLength = toInteger(context, arg2);
        File childFile = new File(filename.toString());
        String filenameString = Helpers.decodeByteList(runtime, filename.getByteList());

        File testFile = childFile.isAbsolute() ? childFile : new File(runtime.getCurrentDirectory(), filenameString);

        if (!testFile.exists()) throw runtime.newErrnoENOENTError(filenameString);
        if (newLength.asLong(context) < 0) throw runtime.newErrnoEINVALError(filenameString);

        IRubyObject[] args = new IRubyObject[] { filename, newString(context, "r+") };
        RubyFile file = (RubyFile) open(context, recv, args, Block.NULL_BLOCK);
        file.truncate(context, newLength);
        file.close();

        return asFixnum(context, 0);
    }

    private static FileSites sites(ThreadContext context) {
        return context.sites.File;
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

    @Deprecated(since = "9.0.0.0")
    protected String path;

    @Deprecated(since = "9.4.6.0")
    public static IRubyObject dirname(ThreadContext context, IRubyObject recv, IRubyObject [] args) {
        return switch (args.length) {
            case 1 -> dirname(context, recv, args[0]);
            case 2 -> dirname(context, recv, args[0], args[1]);
            default -> throw argumentError(context, args.length, 1, 2);
        };
    }

    @Deprecated(since = "9.4.6.0")
    public static IRubyObject expand_path(ThreadContext context, IRubyObject recv, IRubyObject... args) {
        return switch (args.length) {
            case 1 -> expand_path(context, recv, args[0]);
            case 2 -> expand_path(context, recv, args[0], args[1]);
            default -> throw argumentError(context, args.length, 1, 2);
        };
    }

    @Deprecated(since = "9.4.6.0")
    private static RubyString expandPathInternal(ThreadContext context, IRubyObject[] args, boolean expandUser, boolean canonicalize) {
        return switch (args.length) {
            case 1 -> expandPathInternal(context, args[0], null, expandUser, canonicalize);
            case 2 -> expandPathInternal(context, args[0], args[1], expandUser, canonicalize);
            default -> throw argumentError(context, args.length, 1, 2);
        };
    }

    @Deprecated(since = "9.4.6.0")
    public static IRubyObject absolute_path(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        switch (args.length) {
            case 1:
                absolute_path(context, recv, args[0]);
            case 2:
                absolute_path(context, recv, args[0], args[1]);
            default:
                throw argumentError(context, args.length, 1, 2);
        }
    }

    @Deprecated(since = "9.4.6.0")
    public static IRubyObject realdirpath(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        switch (args.length) {
            case 1:
                realdirpath(context, recv, args[0]);
            case 2:
                realdirpath(context, recv, args[0], args[1]);
            default:
                throw argumentError(context, args.length, 1, 2);
        }
    }

    @Deprecated(since = "9.4.6.0")
    public static IRubyObject realpath(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        RubyString file = expandPathInternal(context, args, false, true);
        if (!RubyFileTest.exist(context, file)) throw context.runtime.newErrnoENOENTError(file.toString());
        return file;
    }

    @Deprecated(since = "9.4.6.0")
    public static IRubyObject fnmatch(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return switch (args.length) {
            case 2 -> fnmatch(context, recv, args[0], args[1]);
            case 3 -> fnmatch(context, recv, args[0], args[1], args[2]);
            default -> throw argumentError(context, args.length, 2, 3);
        };
    }
}
