/*
 ***** BEGIN LICENSE BLOCK *****
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
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby;

import jnr.constants.platform.OpenFlags;
import org.jcodings.Encoding;
import org.jruby.util.io.ModeFlags;
import org.jruby.util.io.OpenFile;
import org.jruby.util.io.ChannelDescriptor;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.jcodings.specific.ASCIIEncoding;

import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyModule;
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
import org.jruby.util.io.DirectoryAsFileException;
import org.jruby.util.io.PermissionDeniedException;
import org.jruby.util.io.Stream;
import org.jruby.util.io.ChannelStream;
import org.jruby.util.io.IOOptions;
import org.jruby.util.JRubyFile;
import org.jruby.util.TypeConverter;
import org.jruby.util.io.BadDescriptorException;
import org.jruby.util.io.FileExistsException;
import org.jruby.util.io.InvalidValueException;
import org.jruby.util.io.PipeException;
import static org.jruby.CompatVersion.*;

/**
 * Ruby File class equivalent in java.
 **/
@JRubyClass(name="File", parent="IO", include="FileTest")
public class RubyFile extends RubyIO implements EncodingCapable {
    private static final long serialVersionUID = 1L;
    
    public static final int LOCK_SH = 1;
    public static final int LOCK_EX = 2;
    public static final int LOCK_NB = 4;
    public static final int LOCK_UN = 8;

    private static final int FNM_NOESCAPE = 1;
    private static final int FNM_PATHNAME = 2;
    private static final int FNM_DOTMATCH = 4;
    private static final int FNM_CASEFOLD = 8;
    private static final int FNM_SYSCASE;

    private static int _cachedUmask = 0;
    private static final Object _umaskLock = new Object();

    static {
        if (Platform.IS_WINDOWS) {
            FNM_SYSCASE = FNM_CASEFOLD;
        } else {
            FNM_SYSCASE = 0;
        }
    }

    public Encoding getEncoding() {
        return null;
    }

    public void setEncoding(Encoding encoding) {
        // :)
    }

    private static boolean startsWithDriveLetterOnWindows(String path) {
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

    protected String path;
    private volatile FileLock currentLock;
    
    public RubyFile(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }
    
    // XXX This constructor is a hack to implement the __END__ syntax.
    //     Converting a reader back into an InputStream doesn't generally work.
    public RubyFile(Ruby runtime, String path, final Reader reader) {
        this(runtime, path, new InputStream() {
            public int read() throws IOException {
                return reader.read();
            }
        });
    }
    
    public RubyFile(Ruby runtime, String path, InputStream in) {
        super(runtime, runtime.getFile());
        this.path = path;
        try {
            this.openFile.setMainStream(ChannelStream.open(runtime, new ChannelDescriptor(Channels.newChannel(in))));
            this.openFile.setMode(openFile.getMainStreamSafe().getModes().getOpenFileFlags());
        } catch (BadDescriptorException e) {
            throw runtime.newErrnoEBADFError();
        } catch (InvalidValueException ex) {
            throw runtime.newErrnoEINVALError();
        }
    }

    private static ObjectAllocator FILE_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            RubyFile instance = new RubyFile(runtime, klass);
            
            instance.setMetaClass(klass);
            
            return instance;
        }
    };

    public String getPath() {
        return path;
    }

    @JRubyModule(name="File::Constants")
    public static class Constants {}
    
    public static RubyClass createFileClass(Ruby runtime) {
        RubyClass fileClass = runtime.defineClass("File", runtime.getIO(), FILE_ALLOCATOR);
        // Create Constants class
        RubyModule constants = fileClass.defineModuleUnder("Constants");
        runtime.setFile(fileClass);

        fileClass.index = ClassIndex.FILE;
        fileClass.setReifiedClass(RubyFile.class);

        RubyString separator = runtime.newString("/");
        ThreadContext context = runtime.getCurrentContext();
        
        fileClass.kindOf = new RubyModule.KindOf() {
            @Override
            public boolean isKindOf(IRubyObject obj, RubyModule type) {
                return obj instanceof RubyFile;
            }
        };

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
        
        RubyString pathSeparator = runtime.newString(File.pathSeparator);
        pathSeparator.freeze(context);
        fileClass.defineConstant("PATH_SEPARATOR", pathSeparator);

        // TODO: why are we duplicating the constants here, and then in
        // File::Constants below? File::Constants is included in IO.

        // TODO: These were missing, so we're not handling them elsewhere?
        fileClass.setConstant("FNM_NOESCAPE", runtime.newFixnum(FNM_NOESCAPE));
        fileClass.setConstant("FNM_CASEFOLD", runtime.newFixnum(FNM_CASEFOLD));
        fileClass.setConstant("FNM_SYSCASE", runtime.newFixnum(FNM_SYSCASE));
        fileClass.setConstant("FNM_DOTMATCH", runtime.newFixnum(FNM_DOTMATCH));
        fileClass.setConstant("FNM_PATHNAME", runtime.newFixnum(FNM_PATHNAME));
        
        // Create constants for open flags
        for (OpenFlags f : OpenFlags.values()) {
            // Strip off the O_ prefix, so they become File::RDONLY, and so on
            final String name = f.name();
            if (name.startsWith("O_")) {
                final String cname = name.substring(2);
                // Special case for handling ACCMODE, since constantine will generate
                // an invalid value if it is not defined by the platform.
                final RubyFixnum cvalue = f == OpenFlags.O_ACCMODE
                        ? runtime.newFixnum(ModeFlags.ACCMODE)
                        : runtime.newFixnum(f.intValue());
                fileClass.setConstant(cname, cvalue);
                constants.setConstant(cname, cvalue);
            }
        }
        
        // Create constants for flock
        fileClass.setConstant("LOCK_SH", runtime.newFixnum(RubyFile.LOCK_SH));
        fileClass.setConstant("LOCK_EX", runtime.newFixnum(RubyFile.LOCK_EX));
        fileClass.setConstant("LOCK_NB", runtime.newFixnum(RubyFile.LOCK_NB));
        fileClass.setConstant("LOCK_UN", runtime.newFixnum(RubyFile.LOCK_UN));
        
        constants.setConstant("FNM_NOESCAPE", runtime.newFixnum(FNM_NOESCAPE));
        constants.setConstant("FNM_CASEFOLD", runtime.newFixnum(FNM_CASEFOLD));
        constants.setConstant("FNM_SYSCASE", runtime.newFixnum(FNM_SYSCASE));
        constants.setConstant("FNM_DOTMATCH", runtime.newFixnum(FNM_DOTMATCH));
        constants.setConstant("FNM_PATHNAME", runtime.newFixnum(FNM_PATHNAME));
        
        // Create constants for flock
        constants.setConstant("LOCK_SH", runtime.newFixnum(RubyFile.LOCK_SH));
        constants.setConstant("LOCK_EX", runtime.newFixnum(RubyFile.LOCK_EX));
        constants.setConstant("LOCK_NB", runtime.newFixnum(RubyFile.LOCK_NB));
        constants.setConstant("LOCK_UN", runtime.newFixnum(RubyFile.LOCK_UN));
        
        // File::Constants module is included in IO.
        runtime.getIO().includeModule(constants);

        runtime.getFileTest().extend_object(fileClass);
        
        fileClass.defineAnnotatedMethods(RubyFile.class);

        // For JRUBY-5276, physically define FileTest methods on File's singleton
        fileClass.getSingletonClass().defineAnnotatedMethods(RubyFileTest.FileTestFileMethods.class);
        
        return fileClass;
    }
    
    @JRubyMethod
    @Override
    public IRubyObject close() {
        // Make sure any existing lock is released before we try and close the file
        if (currentLock != null) {
            try {
                currentLock.release();
            } catch (IOException e) {
                throw getRuntime().newIOError(e.getMessage());
            }
        }
        return super.close();
    }

    @JRubyMethod(required = 1)
    public IRubyObject flock(ThreadContext context, IRubyObject lockingConstant) {
        Ruby runtime = context.runtime;
        
        // TODO: port exact behavior from MRI, and move most locking logic into ChannelDescriptor
        // TODO: for all LOCK_NB cases, return false if they would block
        ChannelDescriptor descriptor;
        try {
            descriptor = openFile.getMainStreamSafe().getDescriptor();
        } catch (BadDescriptorException e) {
            throw context.runtime.newErrnoEBADFError();
        }

        // null channel always succeeds for all locking operations
        if (descriptor.isNull()) return RubyFixnum.zero(runtime);

        if (descriptor.getChannel() instanceof FileChannel) {
            FileChannel fileChannel = (FileChannel)descriptor.getChannel();
            int lockMode = RubyNumeric.num2int(lockingConstant);

            checkSharedExclusive(runtime, openFile, lockMode);
    
            if (!lockStateChanges(currentLock, lockMode)) return RubyFixnum.zero(runtime);

            try {
                synchronized (fileChannel) {
                    // check again, to avoid unnecessary overhead
                    if (!lockStateChanges(currentLock, lockMode)) return RubyFixnum.zero(runtime);
                    
                    switch (lockMode) {
                        case LOCK_UN:
                        case LOCK_UN | LOCK_NB:
                            return unlock(runtime);
                        case LOCK_EX:
                            return lock(runtime, fileChannel, true);
                        case LOCK_EX | LOCK_NB:
                            return tryLock(runtime, fileChannel, true);
                        case LOCK_SH:
                            return lock(runtime, fileChannel, false);
                        case LOCK_SH | LOCK_NB:
                            return tryLock(runtime, fileChannel, false);
                    }
                }
            } catch (IOException ioe) {
                if (runtime.getDebug().isTrue()) {
                    ioe.printStackTrace(System.err);
                }
            } catch (java.nio.channels.OverlappingFileLockException ioe) {
                if (runtime.getDebug().isTrue()) {
                    ioe.printStackTrace(System.err);
                }
            }
            return lockFailedReturn(runtime, lockMode);
        } else {
            // We're not actually a real file, so we can't flock
            return runtime.getFalse();
        }
    }
    
    private static void checkSharedExclusive(Ruby runtime, OpenFile openFile, int lockMode) {
        // This logic used to attempt a shared lock instead of an exclusive
        // lock, because LOCK_EX on some systems (as reported in JRUBY-1214)
        // allow exclusively locking a read-only file. However, the JDK
        // APIs do not allow acquiring an exclusive lock on files that are
        // not open for read, and there are other platforms (such as Solaris,
        // see JRUBY-5627) that refuse at an *OS* level to exclusively lock
        // files opened only for read. As a result, this behavior is platform-
        // dependent, and so we will obey the JDK's policy of disallowing
        // exclusive locks on files opened only for read.
        if (!openFile.isWritable() && (lockMode & LOCK_EX) > 0) {
            throw runtime.newErrnoEBADFError("cannot acquire exclusive lock on File not opened for write");
        }
        
        // Likewise, JDK does not allow acquiring a shared lock on files
        // that have not been opened for read. We comply here.
        if (!openFile.isReadable() && (lockMode & LOCK_SH) > 0) {
            throw runtime.newErrnoEBADFError("cannot acquire shared lock on File not opened for read");
        }
    }
    
    private static IRubyObject lockFailedReturn(Ruby runtime, int lockMode) {
        return (lockMode & LOCK_EX) == 0 ? RubyFixnum.zero(runtime) : runtime.getFalse();
    }
    
    private static boolean lockStateChanges(FileLock lock, int lockMode) {
        if (lock == null) {
            // no lock, only proceed if we are acquiring
            switch (lockMode & 0xF) {
                case LOCK_UN:
                case LOCK_UN | LOCK_NB:
                    return false;
                default:
                    return true;
            }
        } else {
            // existing lock, only proceed if we are unlocking or changing
            switch (lockMode & 0xF) {
                case LOCK_UN:
                case LOCK_UN | LOCK_NB:
                    return true;
                case LOCK_EX:
                case LOCK_EX | LOCK_NB:
                    return lock.isShared();
                case LOCK_SH:
                case LOCK_SH | LOCK_NB:
                    return !lock.isShared();
                default:
                    return false;
            }
        }
    }
    
    private IRubyObject unlock(Ruby runtime) throws IOException {
        if (currentLock != null) {
            currentLock.release();
            currentLock = null;

            return RubyFixnum.zero(runtime);
        }
        return runtime.getFalse();
    }
    
    private IRubyObject lock(Ruby runtime, FileChannel fileChannel, boolean exclusive) throws IOException {
        if (currentLock != null) currentLock.release();
        
        currentLock = fileChannel.lock(0L, Long.MAX_VALUE, !exclusive);
        
        if (currentLock != null) {
            return RubyFixnum.zero(runtime);
        }
        
        return lockFailedReturn(runtime, exclusive ? LOCK_EX : LOCK_SH);
    }
    
    private IRubyObject tryLock(Ruby runtime, FileChannel fileChannel, boolean exclusive) throws IOException {
        if (currentLock != null) currentLock.release();
        
        currentLock = fileChannel.tryLock(0L, Long.MAX_VALUE, !exclusive);
        
        if (currentLock != null) {
            return RubyFixnum.zero(runtime);
        }
        
        return lockFailedReturn(runtime, exclusive ? LOCK_EX : LOCK_SH);
    }

    @JRubyMethod(required = 1, optional = 2, visibility = PRIVATE, compat = RUBY1_8)
    @Override
    public IRubyObject initialize(IRubyObject[] args, Block block) {
        if (openFile == null) {
            throw getRuntime().newRuntimeError("reinitializing File");
        }
        
        if (args.length > 0 && args.length < 3) {
            if (args[0] instanceof RubyInteger) {
                return super.initialize(args, block);
            }
        }

        return openFile(args);
    }

    @JRubyMethod(name = "initialize", required = 1, optional = 2, visibility = PRIVATE, compat = RUBY1_9)
    public IRubyObject initialize19(ThreadContext context, IRubyObject[] args, Block block) {
        if (openFile == null) {
            throw context.getRuntime().newRuntimeError("reinitializing File");
        }

        if (args.length > 0 && args.length <= 3) {
            IRubyObject fd = TypeConverter.convertToTypeWithCheck(args[0], context.getRuntime().getFixnum(), "to_int");
            if (!fd.isNil()) {
                args[0] = fd;
                if (args.length == 1) {
                    return super.initialize19(context, args[0], block);
                } else if (args.length == 2) {
                    return super.initialize19(context, args[0], args[1], block);
                }
                return super.initialize19(context, args[0], args[1], args[2], block);
            }
        }

        return openFile19(context, args);
    }

    private IRubyObject openFile19(ThreadContext context, IRubyObject args[]) {
        Ruby runtime = context.getRuntime();
        RubyString filename = get_path(context, args[0]);
        runtime.checkSafeString(filename);

        path = adjustRootPathOnWindows(runtime, filename.getUnicodeValue(), runtime.getCurrentDirectory());

        String modeString = "r";
        IOOptions modes = newIOOptions(runtime, modeString);
        RubyHash options = null;
        int perm = 0;

        if (args.length > 1) {
            if (args[1] instanceof RubyHash) {
                options = (RubyHash)args[1];
            } else {
                modes = parseIOOptions19(args[1]);

                if (args[1] instanceof RubyFixnum) {
                    perm = getFilePermissions(args);
                } else {
                    modeString = args[1].convertToString().toString();
                }
            }
        } else {
            modes = parseIOOptions19(RubyString.newString(runtime, modeString));
        }

        if (args.length > 2 && !args[2].isNil()) {
            if (args[2] instanceof RubyHash) {
                options = (RubyHash)args[2];
            } else {
                perm = getFilePermissions(args);
            }
        }

        if (perm > 0) {
            sysopenInternal19(context, path, options, modes, perm);
        } else {
            openInternal19(context, path, options, modeString, modes);
        }

        return this;
    }
    
    private IRubyObject openFile(IRubyObject args[]) {
        Ruby runtime = getRuntime();
        RubyString filename = get_path(runtime.getCurrentContext(), args[0]);
        runtime.checkSafeString(filename);
        
        path = adjustRootPathOnWindows(runtime, filename.getUnicodeValue(), runtime.getCurrentDirectory());
        
        String modeString;
        IOOptions modes;
        int perm;
        
        if ((args.length > 1 && args[1] instanceof RubyFixnum) || (args.length > 2 && !args[2].isNil())) {
            modes = parseIOOptions(args[1]);
            perm = getFilePermissions(args);

            sysopenInternal(path, modes.getModeFlags(), perm);
        } else {
            modeString = "r";
            if (args.length > 1 && !args[1].isNil()) {
                modeString = args[1].convertToString().toString();
            }

            openInternal(path, modeString);
        }
        
        return this;
    }

    private int getFilePermissions(IRubyObject[] args) {
        return (args.length > 2 && !args[2].isNil()) ? RubyNumeric.num2int(args[2]) : 438;
    }

    protected void sysopenInternal19(ThreadContext context, String path, RubyHash options, IOOptions ioOptions, int perm) {
        ioOptions = updateIOOptionsFromOptions(context, options, ioOptions);

        sysopenInternal(path, ioOptions.getModeFlags(), perm);

        setEncodingFromOptions(ioOptions.getEncodingOption());
    }

    protected void sysopenInternal(String path, ModeFlags modes, int perm) {
        openFile = new OpenFile();

        openFile.setPath(path);
        openFile.setMode(modes.getOpenFileFlags());
        if (modes.isBinary()) externalEncoding = ASCIIEncoding.INSTANCE;

        int umask = getUmaskSafe( getRuntime() );
        perm = perm - (perm & umask);

        ChannelDescriptor descriptor = sysopen(path, modes, perm);
        openFile.setMainStream(fdopen(descriptor, modes));
    }

    protected void openInternal19(ThreadContext context, String path, RubyHash options, String modeString, IOOptions ioOptions) {
        ioOptions = updateIOOptionsFromOptions(context, options, ioOptions);

        openInternal(path, modeString, ioOptions.getModeFlags());

        setEncodingFromOptions(ioOptions.getEncodingOption());
    }

    protected void openInternal(String path, String modeString, ModeFlags modes) {
        openFile = new OpenFile();

        openFile.setMode(modes.getOpenFileFlags());
        if (modes.isBinary()) externalEncoding = ASCIIEncoding.INSTANCE;
        openFile.setPath(path);
        openFile.setMainStream(fopen(path, modeString));
    }
    
    protected void openInternal(String path, String modeString) {
        openFile = new OpenFile();

        IOOptions modes = newIOOptions(getRuntime(), modeString);
        openFile.setMode(modes.getModeFlags().getOpenFileFlags());
        if (modes.getModeFlags().isBinary()) externalEncoding = ASCIIEncoding.INSTANCE;
        openFile.setPath(path);
        openFile.setMainStream(fopen(path, modeString));
    }
    
    private ChannelDescriptor sysopen(String path, ModeFlags modes, int perm) {
        try {
            ChannelDescriptor descriptor = ChannelDescriptor.open(
                    getRuntime().getCurrentDirectory(),
                    path,
                    modes,
                    perm,
                    getRuntime().getPosix(),
                    getRuntime().getJRubyClassLoader());

            // TODO: check if too many open files, GC and try again

            return descriptor;
        } catch (PermissionDeniedException pde) {
            // PDException can be thrown only when creating the file and
            // permission is denied.  See JavaDoc of PermissionDeniedException.
            throw getRuntime().newErrnoEACCESError(path);
        } catch (FileNotFoundException fnfe) {
            // FNFException can be thrown in both cases, when the file
            // is not found, or when permission is denied.
            if (Ruby.isSecurityRestricted() || new File(path).exists()) {
                throw getRuntime().newErrnoEACCESError(path);
            }
            throw getRuntime().newErrnoENOENTError(path);
        } catch (DirectoryAsFileException dafe) {
            throw getRuntime().newErrnoEISDirError();
        } catch (FileExistsException fee) {
            throw getRuntime().newErrnoEEXISTError(path);
        } catch (IOException ioe) {
            throw getRuntime().newIOErrorFromException(ioe);
        }
    }
    
    private Stream fopen(String path, String modeString) {
        try {
            Stream stream = ChannelStream.fopen(
                    getRuntime(),
                    path,
                    newModeFlags(getRuntime(), modeString));
            
            if (stream == null) {
                // TODO
    //            if (errno == EMFILE || errno == ENFILE) {
    //                rb_gc();
    //                file = fopen(fname, mode);
    //            }
    //            if (!file) {
    //                rb_sys_fail(fname);
    //            }
            }

            // Do we need to be in SETVBUF mode for buffering to make sense? This comes up elsewhere.
    //    #ifdef USE_SETVBUF
    //        if (setvbuf(file, NULL, _IOFBF, 0) != 0)
    //            rb_warn("setvbuf() can't be honoured for %s", fname);
    //    #endif
    //    #ifdef __human68k__
    //        fmode(file, _IOTEXT);
    //    #endif
            return stream;
        } catch (BadDescriptorException e) {
            throw getRuntime().newErrnoEBADFError();
        } catch (PermissionDeniedException pde) {
            // PDException can be thrown only when creating the file and
            // permission is denied.  See JavaDoc of PermissionDeniedException.
            throw getRuntime().newErrnoEACCESError(path);
        } catch (FileNotFoundException ex) {
            // FNFException can be thrown in both cases, when the file
            // is not found, or when permission is denied.
            if (Ruby.isSecurityRestricted() || new File(path).exists()) {
                throw getRuntime().newErrnoEACCESError(path);
            }
            throw getRuntime().newErrnoENOENTError(path);
        } catch (DirectoryAsFileException ex) {
            throw getRuntime().newErrnoEISDirError();
        } catch (FileExistsException ex) {
            throw getRuntime().newErrnoEEXISTError(path);
        } catch (IOException ex) {
            throw getRuntime().newIOErrorFromException(ex);
        } catch (InvalidValueException ex) {
            throw getRuntime().newErrnoEINVALError();
        } catch (PipeException ex) {
            throw getRuntime().newErrnoEPIPEError();
        } catch (SecurityException ex) {
            throw getRuntime().newErrnoEACCESError(path);
        }
    }

    @JRubyMethod(required = 1)
    public IRubyObject chmod(ThreadContext context, IRubyObject arg) {
        checkClosed(context);
        int mode = (int) arg.convertToInteger().getLongValue();

        if (!new File(path).exists()) {
            throw context.getRuntime().newErrnoENOENTError(path);
        }

        return context.getRuntime().newFixnum(context.getRuntime().getPosix().chmod(path, mode));
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

        if (!new File(path).exists()) {
            throw context.getRuntime().newErrnoENOENTError(path);
        }

        return context.getRuntime().newFixnum(context.getRuntime().getPosix().chown(path, owner, group));
    }

    @JRubyMethod
    public IRubyObject atime(ThreadContext context) {
        checkClosed(context);
        return context.getRuntime().newFileStat(path, false).atime();
    }

    @JRubyMethod
    public IRubyObject ctime(ThreadContext context) {
        checkClosed(context);
        return context.getRuntime().newFileStat(path, false).ctime();
    }

    @JRubyMethod(required = 1)
    public IRubyObject lchmod(ThreadContext context, IRubyObject arg) {
        int mode = (int) arg.convertToInteger().getLongValue();

        if (!new File(path).exists()) {
            throw context.getRuntime().newErrnoENOENTError(path);
        }

        return context.getRuntime().newFixnum(context.getRuntime().getPosix().lchmod(path, mode));
    }

    // TODO: this method is not present in MRI!
    @JRubyMethod(required = 2)
    public IRubyObject lchown(ThreadContext context, IRubyObject arg1, IRubyObject arg2) {
        int owner = -1;
        if (!arg1.isNil()) {
            owner = RubyNumeric.num2int(arg1);
        }

        int group = -1;
        if (!arg2.isNil()) {
            group = RubyNumeric.num2int(arg2);
        }

        if (!new File(path).exists()) {
            throw context.getRuntime().newErrnoENOENTError(path);
        }

        return context.getRuntime().newFixnum(context.getRuntime().getPosix().lchown(path, owner, group));
    }

    @JRubyMethod
    public IRubyObject lstat(ThreadContext context) {
        checkClosed(context);
        return context.getRuntime().newFileStat(path, true);
    }
    
    @JRubyMethod
    public IRubyObject mtime(ThreadContext context) {
        checkClosed(context);
        return getLastModified(context.getRuntime(), path);
    }

    @JRubyMethod(meta = true, compat = RUBY1_9)
    public static IRubyObject path(ThreadContext context, IRubyObject self, IRubyObject str) {
        return get_path(context, str);
    }

    /**
     * similar in spirit to rb_get_path from 1.9 source
     * @param context
     * @param obj
     * @return
     */
    public static RubyString get_path(ThreadContext context, IRubyObject obj) {
        if (context.getRuntime().is1_9()) {
            if (obj instanceof RubyString) {
                return (RubyString)obj;
            }
            
            if (obj.respondsTo("to_path")) {
                obj = obj.callMethod(context, "to_path");
            }
        }

        return obj.convertToString();
    }

    /**
     * Get the fully-qualified JRubyFile object for the path, taking into
     * account the runtime's current directory.
     */
    public static JRubyFile file(IRubyObject pathOrFile) {
        Ruby runtime = pathOrFile.getRuntime();

        if (pathOrFile instanceof RubyFile) {
            return JRubyFile.create(runtime.getCurrentDirectory(), ((RubyFile) pathOrFile).getPath());
        } else {
            RubyString pathStr = get_path(runtime.getCurrentContext(), pathOrFile);
            String path = pathStr.getUnicodeValue();
            String[] pathParts = splitURI(path);
            if (pathParts != null && pathParts[0].equals("file:")) {
                path = pathParts[1];
            }
            return JRubyFile.create(runtime.getCurrentDirectory(), path);
        }
    }

    @JRubyMethod(name = {"path", "to_path"})
    public IRubyObject path(ThreadContext context) {
        IRubyObject newPath = context.getRuntime().getNil();
        if (path != null) {
            newPath = context.getRuntime().newString(path);
            newPath.setTaint(true);
        }
        return newPath;
    }

    @JRubyMethod
    @Override
    public IRubyObject stat(ThreadContext context) {
        checkClosed(context);
        return context.getRuntime().newFileStat(path, false);
    }

    @JRubyMethod(required = 1)
    public IRubyObject truncate(ThreadContext context, IRubyObject arg) {
        RubyInteger newLength = arg.convertToInteger();
        if (newLength.getLongValue() < 0) {
            throw context.getRuntime().newErrnoEINVALError(path);
        }
        try {
            openFile.checkWritable(context.getRuntime());
            openFile.getMainStreamSafe().ftruncate(newLength.getLongValue());
        } catch (BadDescriptorException e) {
            throw context.getRuntime().newErrnoEBADFError();
        } catch (PipeException e) {
            throw context.getRuntime().newErrnoESPIPEError();
        } catch (InvalidValueException ex) {
            throw context.getRuntime().newErrnoEINVALError();
        } catch (IOException e) {
            // Should we do anything?
        }

        return RubyFixnum.zero(context.getRuntime());
    }

    @Override
    public String toString() {
        try {
            return "RubyFile(" + path + ", " + openFile.getMode() + ", " + getRuntime().getFileno(openFile.getMainStreamSafe().getDescriptor()) + ")";
        } catch (BadDescriptorException e) {
            throw getRuntime().newErrnoEBADFError();
        }
    }

    @JRubyMethod
    @Override
    public IRubyObject inspect() {
        StringBuilder val = new StringBuilder();
        val.append("#<File:").append(path);
        if(!openFile.isOpen()) {
            val.append(" (closed)");
        }
        val.append(">");
        return getRuntime().newString(val.toString());
    }
    
    /* File class methods */
    
    @JRubyMethod(required = 1, optional = 1, meta = true)
    public static IRubyObject basename(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        String name = get_path(context,args[0]).getUnicodeValue();

        // MRI-compatible basename handling for windows drive letter paths
        if (Platform.IS_WINDOWS) {
            if (name.length() > 1 && name.charAt(1) == ':' && Character.isLetter(name.charAt(0))) {
                switch (name.length()) {
                case 2:
                    return RubyString.newEmptyString(context.getRuntime()).infectBy(args[0]);
                case 3:
                    return context.getRuntime().newString(name.substring(2)).infectBy(args[0]);
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
        return context.getRuntime().newString(name).infectBy(args[0]);
    }

    @JRubyMethod(required = 2, rest = true, meta = true)
    public static IRubyObject chmod(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = context.getRuntime();
        
        int count = 0;
        RubyInteger mode = args[0].convertToInteger();
        for (int i = 1; i < args.length; i++) {
            JRubyFile filename = file(args[i]);
            
            if (!filename.exists()) {
                throw runtime.newErrnoENOENTError(filename.toString());
            }
            
            boolean result = 0 == runtime.getPosix().chmod(filename.getAbsolutePath(), (int)mode.getLongValue());
            if (result) {
                count++;
            }
        }
        
        return runtime.newFixnum(count);
    }
    
    @JRubyMethod(required = 3, rest = true, meta = true)
    public static IRubyObject chown(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = context.getRuntime();

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
            
            boolean result = 0 == runtime.getPosix().chown(filename.getAbsolutePath(), owner, group);
            if (result) {
                count++;
            }
        }
        
        return runtime.newFixnum(count);
    }
    
    @JRubyMethod(required = 1, meta = true)
    public static IRubyObject dirname(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        RubyString filename = get_path(context, arg);
        
        String jfilename = filename.getUnicodeValue();
        
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
                    return context.getRuntime().newString(jfilename.substring(0, 2) + ".");
                } else {
                    return context.getRuntime().newString(".");
                }
            }
            if (index == 0) return context.getRuntime().newString("/");

            if (startsWithDriveLetterOnWindows && index == 2) {
                // Include additional path separator
                // (so that dirname of "C:\file.txt" is  "C:\", not "C:")
                index++;
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

        return context.getRuntime().newString(result).infectBy(filename);
    }

    private static boolean isWindowsDriveLetter(char c) {
           return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
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

        return context.getRuntime().newString(result);
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
    @JRubyMethod(required = 1, optional = 1, meta = true, compat = CompatVersion.RUBY1_8)
    public static IRubyObject expand_path(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return expandPathInternal(context, recv, args, true);
    }

    @JRubyMethod(name = "expand_path", required = 1, optional = 1, meta = true, compat = CompatVersion.RUBY1_9)
    public static IRubyObject expand_path19(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        RubyString path = (RubyString) expandPathInternal(context, recv, args, true);
        path.force_encoding(context, context.getRuntime().getEncodingService().getDefaultExternal());

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
    @JRubyMethod(required = 1, optional = 1, meta = true, compat = RUBY1_9)
    public static IRubyObject absolute_path(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return expandPathInternal(context, recv, args, false);
    }

    @JRubyMethod(name = {"realdirpath"}, required = 1, optional = 1, meta = true, compat = RUBY1_9)
    public static IRubyObject realdirpath(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return expandPathInternal(context, recv, args, false);
    }

    @JRubyMethod(name = {"realpath"}, required = 1, optional = 1, meta = true, compat = RUBY1_9)
    public static IRubyObject realpath(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        IRubyObject file = expandPathInternal(context, recv, args, false);
        if (!RubyFileTest.exist_p(recv, file).isTrue()) {
            throw context.getRuntime().newErrnoENOENTError(file.toString());
        }
        return file;
    }

    private static IRubyObject expandPathInternal(ThreadContext context, IRubyObject recv, IRubyObject[] args, boolean expandUser) {
        Ruby runtime = context.getRuntime();

        String relativePath = get_path(context, args[0]).getUnicodeValue();
        String[] uriParts = splitURI(relativePath);
        String cwd = null;

        // Handle ~user paths 
        if (expandUser) {
            relativePath = expandUserPath(context, relativePath);
        }

        if (uriParts != null) {
            relativePath = uriParts[1];
        }

        // If there's a second argument, it's the path to which the first
        // argument is relative.
        if (args.length == 2 && !args[1].isNil()) {
            cwd = get_path(context, args[1]).getUnicodeValue();

            // Handle ~user paths.
            if (expandUser) {
                cwd = expandUserPath(context, cwd);
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

        return runtime.newString(padSlashes + canonicalize(path.getAbsolutePath()));
    }

    private static Pattern URI_PREFIX = Pattern.compile("^(jar:)?[a-z]{2,}:(.*)");
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
        int pathLength = path.length();

        if (pathLength >= 1 && path.charAt(0) == '~') {
            // Enebo : Should ~frogger\\foo work (it doesnt in linux ruby)?
            int userEnd = path.indexOf('/');
            
            if (userEnd == -1) {
                if (pathLength == 1) {
                    // Single '~' as whole path to expand
                    checkHome(context);
                    path = RubyDir.getHomeDirectoryPath(context).toString();
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
            } else if (userEnd > 1){
                // '~user/...' as path to expand
                String user = path.substring(1, userEnd);
                IRubyObject dir = RubyDir.getHomeDirectoryPath(context, user);
                
                if (dir.isNil()) {
                    throw context.getRuntime().newArgumentError("user " + user + " does not exist");
                }
                
                path = "" + dir + (pathLength == userEnd ? "" : path.substring(userEnd));
            }
        }
        return path;
    }

    private static final String[] SLASHES = {"", "/", "//"};
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
        int flags = args.length == 3 ? RubyNumeric.num2int(args[2]) : 0;

        ByteList pattern = args[0].convertToString().getByteList();
        ByteList path = get_path(context, args[1]).getByteList();

        if (org.jruby.util.Dir.fnmatch(pattern.getUnsafeBytes(), pattern.getBegin(), pattern.getBegin()+pattern.getRealSize(), path.getUnsafeBytes(), path.getBegin(), path.getBegin()+path.getRealSize(), flags) == 0) {
            return context.getRuntime().getTrue();
        }
        return context.getRuntime().getFalse();
    }
    
    @JRubyMethod(name = "ftype", required = 1, meta = true)
    public static IRubyObject ftype(ThreadContext context, IRubyObject recv, IRubyObject filename) {
        return context.getRuntime().newFileStat(get_path(context, filename).getUnicodeValue(), true).ftype();
    }

    private static String inspectJoin(ThreadContext context, IRubyObject recv, RubyArray parent, RubyArray array) {
        Ruby runtime = context.getRuntime();
        
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
        Ruby runtime = context.getRuntime();
        
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
                RubyString path = get_path(context, args[i]);
                element = path.getUnicodeValue();
            }
            
            chomp(buffer);
            if (i > 0 && !element.startsWith("/") && !element.startsWith("\\")) {
                buffer.append("/");
            }
            buffer.append(element);
        }
        
        RubyString fixedStr = RubyString.newString(runtime, buffer.toString());
        fixedStr.setTaint(isTainted);
        return fixedStr;
    }
    
    /*
     * Fixme:  This does not have exact same semantics as RubyArray.join, but they
     * probably could be consolidated (perhaps as join(args[], sep, doChomp)).
     */
    @JRubyMethod(rest = true, meta = true)
    public static RubyString join(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return join(context, recv, RubyArray.newArrayNoCopyLight(context.getRuntime(), args));
    }

    private static void chomp(StringBuilder buffer) {
        int lastIndex = buffer.length() - 1;
        
        while (lastIndex >= 0 && (buffer.lastIndexOf("/") == lastIndex || buffer.lastIndexOf("\\") == lastIndex)) {
            buffer.setLength(lastIndex);
            lastIndex--;
        }
    }
    
    @JRubyMethod(name = "lstat", required = 1, meta = true)
    public static IRubyObject lstat(ThreadContext context, IRubyObject recv, IRubyObject filename) {
        String f = get_path(context, filename).getUnicodeValue();
        return context.getRuntime().newFileStat(f, true);
    }

    @JRubyMethod(name = "stat", required = 1, meta = true)
    public static IRubyObject stat(ThreadContext context, IRubyObject recv, IRubyObject filename) {
        String f = get_path(context, filename).getUnicodeValue();
        return context.getRuntime().newFileStat(f, false);
    }

    @JRubyMethod(name = "atime", required = 1, meta = true)
    public static IRubyObject atime(ThreadContext context, IRubyObject recv, IRubyObject filename) {
        String f = get_path(context, filename).getUnicodeValue();
        return context.getRuntime().newFileStat(f, false).atime();
    }

    @JRubyMethod(name = "ctime", required = 1, meta = true)
    public static IRubyObject ctime(ThreadContext context, IRubyObject recv, IRubyObject filename) {
        String f = get_path(context, filename).getUnicodeValue();
        return context.getRuntime().newFileStat(f, false).ctime();
    }

    @JRubyMethod(required = 2, rest = true, meta = true)
    public static IRubyObject lchmod(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = context.getRuntime();
        
        int count = 0;
        RubyInteger mode = args[0].convertToInteger();
        for (int i = 1; i < args.length; i++) {
            RubyString filename = get_path(context, args[i]);
            
            if (!RubyFileTest.exist_p(filename, filename).isTrue()) {
                throw runtime.newErrnoENOENTError(filename.toString());
            }
            
            boolean result = 0 == runtime.getPosix().lchmod(filename.getUnicodeValue(), (int)mode.getLongValue());
            if (result) {
                count++;
            }
        }
        
        return runtime.newFixnum(count);
    }

    @JRubyMethod(required = 2, rest = true, meta = true)
    public static IRubyObject lchown(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = context.getRuntime();
        int owner = !args[0].isNil() ? RubyNumeric.num2int(args[0]) : -1;
        int group = !args[1].isNil() ? RubyNumeric.num2int(args[1]) : -1;
        int count = 0;

        for (int i = 2; i < args.length; i++) {
            IRubyObject filename = args[i];

            if (0 != runtime.getPosix().lchown(filename.toString(), owner, group)) {
                throw runtime.newErrnoFromLastPOSIXErrno();
            } else {
                count++;
            }
        }

        return runtime.newFixnum(count);
    }

    @JRubyMethod(required = 2, meta = true)
    public static IRubyObject link(ThreadContext context, IRubyObject recv, IRubyObject from, IRubyObject to) {
        Ruby runtime = context.getRuntime();
        RubyString fromStr = RubyString.stringValue(from);
        RubyString toStr = RubyString.stringValue(to);

        int ret = runtime.getPosix().link(fromStr.getUnicodeValue(), toStr.getUnicodeValue());
        if (ret != 0) {
            // In most cases, when there is an error during the call,
            // the POSIX handler throws an exception, but not in case
            // with pure Java POSIX layer (when native support is disabled),
            // so we deal with it like this:
            throw runtime.newErrnoEEXISTError(fromStr + " or " + toStr);
        }
        return runtime.newFixnum(ret);
    }

    @JRubyMethod(name = "mtime", required = 1, meta = true)
    public static IRubyObject mtime(ThreadContext context, IRubyObject recv, IRubyObject filename) {
        return getLastModified(context.getRuntime(), get_path(context, filename).getUnicodeValue());
    }
    
    @JRubyMethod(required = 2, meta = true)
    public static IRubyObject rename(ThreadContext context, IRubyObject recv, IRubyObject oldName, IRubyObject newName) {
        Ruby runtime = context.getRuntime();
        RubyString oldNameString = RubyString.stringValue(oldName);
        RubyString newNameString = RubyString.stringValue(newName);
        runtime.checkSafeString(oldNameString);
        runtime.checkSafeString(newNameString);

        String newNameJavaString = newNameString.getUnicodeValue();
        String oldNameJavaString = oldNameString.getUnicodeValue();
        JRubyFile oldFile = JRubyFile.create(runtime.getCurrentDirectory(), oldNameJavaString);
        JRubyFile newFile = JRubyFile.create(runtime.getCurrentDirectory(), newNameJavaString);
        
        if (!oldFile.exists() || !newFile.getParentFile().exists()) {
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
        RubyString filename = get_path(context, arg);
        
        return context.getRuntime().newArray(dirname(context, recv, filename),
                basename(context, recv, new IRubyObject[] { filename }));
    }
    
    @JRubyMethod(required = 2, meta = true)
    public static IRubyObject symlink(ThreadContext context, IRubyObject recv, IRubyObject from, IRubyObject to) {
        Ruby runtime = context.getRuntime();
        RubyString fromStr = get_path(context, from);
        RubyString toStr = get_path(context, to);
        String tovalue = toStr.getUnicodeValue();
        tovalue = JRubyFile.create(runtime.getCurrentDirectory(), tovalue).getAbsolutePath();
        try {
            if (runtime.getPosix().symlink(
                    fromStr.getUnicodeValue(), tovalue) == -1) {
                // FIXME: When we get JNA3 we need to properly write this to errno.
                throw runtime.newErrnoEEXISTError(fromStr + " or " + toStr);
            }
        } catch (java.lang.UnsatisfiedLinkError ule) {
            throw runtime.newNotImplementedError("symlink() function is unimplemented on this machine");
        }
        
        return runtime.newFixnum(0);
    }
    
    @JRubyMethod(required = 1, meta = true)
    public static IRubyObject readlink(ThreadContext context, IRubyObject recv, IRubyObject path) {
        Ruby runtime = context.getRuntime();
        
        try {
            String realPath = runtime.getPosix().readlink(path.convertToString().getUnicodeValue());
        
            if (!RubyFileTest.exist_p(recv, path).isTrue()) {
                throw runtime.newErrnoENOENTError(path.toString());
            }
        
            if (!RubyFileTest.symlink_p(recv, path).isTrue()) {
                throw runtime.newErrnoEINVALError(path.toString());
            }
        
            if (realPath == null) {
                //FIXME: When we get JNA3 we need to properly write this to errno.
            }

            return runtime.newString(realPath);
        } catch (IOException e) {
            throw runtime.newIOError(e.getMessage());
        }
    }

    // Can we produce IOError which bypasses a close?
    @JRubyMethod(required = 2, meta = true, compat = RUBY1_8)
    public static IRubyObject truncate(ThreadContext context, IRubyObject recv, IRubyObject arg1, IRubyObject arg2) {        
        return truncateCommon(context, recv, arg1, arg2);
    }

    @JRubyMethod(name = "truncate", required = 2, meta = true, compat = RUBY1_9)
    public static IRubyObject truncate19(ThreadContext context, IRubyObject recv, IRubyObject arg1, IRubyObject arg2) {
        if (!(arg1 instanceof RubyString) && arg1.respondsTo("to_path")) {
            arg1 = arg1.callMethod(context, "to_path");
        }
        return truncateCommon(context, recv, arg1, arg2);
    }

    private static IRubyObject truncateCommon(ThreadContext context, IRubyObject recv, IRubyObject arg1, IRubyObject arg2) {
        RubyString filename = arg1.convertToString(); // TODO: SafeStringValue here
        Ruby runtime = context.getRuntime();
        RubyInteger newLength = arg2.convertToInteger();

        File testFile ;
        File childFile = new File(filename.getUnicodeValue() );

        if ( childFile.isAbsolute() ) {
          testFile = childFile ;
        } else {
          testFile = new File(runtime.getCurrentDirectory(), filename.getByteList().toString());
        }

        if (!testFile.exists()) {
            throw runtime.newErrnoENOENTError(filename.getByteList().toString());
        }

        if (newLength.getLongValue() < 0) {
            throw runtime.newErrnoEINVALError(filename.toString());
        }

        IRubyObject[] args = new IRubyObject[] { filename, runtime.newString("r+") };
        RubyFile file = (RubyFile) open(context, recv, args, Block.NULL_BLOCK);
        file.truncate(context, newLength);
        file.close();

        return RubyFixnum.zero(runtime);
    }

    @JRubyMethod(meta = true, optional = 1)
    public static IRubyObject umask(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = context.getRuntime();
        int oldMask = 0;
        if (args.length == 0) {
            oldMask = getUmaskSafe( runtime );
        } else if (args.length == 1) {
            int newMask = (int) args[0].convertToInteger().getLongValue();
            synchronized (_umaskLock) {
                oldMask = runtime.getPosix().umask(newMask);
                _cachedUmask = newMask;
            }
        } else {
            runtime.newArgumentError("wrong number of arguments");
        }
        
        return runtime.newFixnum(oldMask);
    }

    @JRubyMethod(required = 2, rest = true, meta = true)
    public static IRubyObject utime(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = context.getRuntime();
        long[] atimeval = null;
        long[] mtimeval = null;

        if (args[0] != runtime.getNil() || args[1] != runtime.getNil()) {
            atimeval = extractTimeval(runtime, args[0]);
            mtimeval = extractTimeval(runtime, args[1]);
        }

        for (int i = 2, j = args.length; i < j; i++) {
            RubyString filename = get_path(context, args[i]);
            runtime.checkSafeString(filename);
            
            JRubyFile fileToTouch = JRubyFile.create(runtime.getCurrentDirectory(),filename.getUnicodeValue());
            
            if (!fileToTouch.exists()) {
                throw runtime.newErrnoENOENTError(filename.toString());
            }

            runtime.getPosix().utimes(fileToTouch.getAbsolutePath(), atimeval, mtimeval);
        }
        
        return runtime.newFixnum(args.length - 2);
    }
    
    @JRubyMethod(name = {"unlink", "delete"}, rest = true, meta = true)
    public static IRubyObject unlink(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = context.getRuntime();
         
        for (int i = 0; i < args.length; i++) {
            RubyString filename = get_path(context, args[i]);
            runtime.checkSafeString(filename);
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

    @JRubyMethod(name = "size", compat = RUBY1_9)
    public IRubyObject size(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        if ((openFile.getMode() & OpenFile.WRITABLE) != 0) {
            flush();
        }

        try {
            FileStat stat = runtime.getPosix().fstat(
                    getOpenFileChecked().getMainStreamSafe().getDescriptor().getFileDescriptor());
            if (stat == null) {
                throw runtime.newErrnoEACCESError(path);
            }

            return runtime.newFixnum(stat.st_size());
        } catch (BadDescriptorException e) {
            throw runtime.newErrnoEBADFError();
        }
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
        String dirPath = path + "/";
        ZipFile zf = Ruby.getGlobalRuntime().getCurrentContext().getRuntime().getLoadService().getJarFile(jar);
        ZipEntry entry = zf.getEntry(dirPath); // first try as directory
        if (entry == null) {
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

    /**
     * Joy of POSIX, only way to get the umask is to set the umask,
     * then set it back. That's unsafe in a threaded program. We
     * minimize but may not totally remove this race by caching the
     * obtained or previously set (see umask() above) umask and using
     * that as the initial set value which, cross fingers, is a
     * no-op. The cache access is then synchronized. TODO: Better?
     */
    private static int getUmaskSafe( Ruby runtime ) {
        synchronized (_umaskLock) {
            final int umask = runtime.getPosix().umask(_cachedUmask);
            if (_cachedUmask != umask ) {
                runtime.getPosix().umask(umask);
                _cachedUmask = umask;
            }
            return umask;
        }
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

    // Fast path since JNA stat is about 10x slower than this
    private static IRubyObject getLastModified(Ruby runtime, String path) {
        JRubyFile file = JRubyFile.create(runtime.getCurrentDirectory(), path);
        
        if (!file.exists()) {
            throw runtime.newErrnoENOENTError(path);
        }
        
        return runtime.newTime(file.lastModified());
    }

    private void checkClosed(ThreadContext context) {
        openFile.checkClosed(context.getRuntime());
    }
    
    /**
     * Check if HOME environment variable is not nil nor empty
     * @param context 
     */
    private static void checkHome(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        RubyHash env = runtime.getENV();
        String home = (String) env.get(runtime.newString("HOME"));
        if (home == null || home.equals("")) {
            throw runtime.newArgumentError("couldn't find HOME environment -- expanding `~'");
        }
    }
}
